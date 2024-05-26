import logging
import sys
from typing import List, Dict

import podman.domain.containers
from fastapi import FastAPI
import json
from podman import PodmanClient
from pydantic import BaseModel

app = FastAPI()

pc = PodmanClient(base_url="unix:///run/user/1000/podman/podman.sock")

logger = logging.getLogger(__name__)
logger.setLevel(logging.DEBUG)
handler = logging.StreamHandler()
handler.setLevel(logging.DEBUG)
handler.setStream(sys.stdout)
logger.addHandler(handler)

print("yoooooooooooo")


class ContainerManagerBase:
    def __init__(self, podman_client: PodmanClient, managed_container_name: str):
        self._podman_client = podman_client
        self.managed_container_name = managed_container_name
        self._container_handle: podman.domain.containers.Container | None = None
        # Check, if a container already exists, if so, fetch the handle.
        for container in self._podman_client.containers.list(all=True, filters={"name": self.managed_container_name}):
            self._container_handle = container
            logger.info(f"Found container: {container.name}")
            break

    def pull_image(self, repository, tag) -> str | None:
        logger.info(f"Pulling Image {repository}:{tag}")
        image_id = None
        for line in self._podman_client.images.pull(repository, tag, stream=True):
            # Stream is JSON dicts, decode
            d = json.loads(line)
            if "stream" in d:
                logger.debug(d["stream"])
            elif "images" in d:
                image_id = d["id"]
        logger.info(f"Image {repository}:{tag} pulled successfully.")
        return image_id

    def wait(self, conditions: [str]):
        while True:
            self._container_handle.wait(condition=conditions, interval="1000ms")
            self._container_handle.reload()
            if self._container_handle.status in conditions:
                break
            logger.info(
                f"Container status is {self._container_handle.status}, expected: {conditions} waiting some more.")


class MetaContainerManager(ContainerManagerBase):
    def __init__(self, podman_client: PodmanClient, managed_container_name: str):
        super().__init__(podman_client, managed_container_name)

    def update(self) -> bool:
        # Update the meta container, if we don't have a handle, we cannot update
        if self._container_handle is None:
            logger.warning("No container handle, cannot update meta container image.")
            return False
        # Fetch image repo and tag from the container handle and pull
        image_repo, tag = self._container_handle.attrs["Image"].split(":")
        self.pull_image(image_repo, tag)
        self._container_handle.restart()
        return True


class ContainerManager(ContainerManagerBase):

    def __init__(self, podman_client: PodmanClient, managed_container_name: str, repository: str, tag: str,
                 command: List[str], props: Dict):
        super().__init__(podman_client, managed_container_name)
        self.repository = repository
        self.tag = tag
        self.command = command
        self.props = props

    def stop_container(self):
        if self.__container_handle is None:
            return
        logger.info(f"Stopping and removing container.")
        self.__container_handle.stop(ignore=True)
        self.wait(["stopped", "exited"])

        logger.info(f"Container {self.managed_container_name} stopped.")
        self.__container_handle.remove()
        logger.info(f"Container {self.managed_container_name} removed.")
        self.__container_handle = None
        return

    def get_image_id(self, force_pull: bool = False) -> str | None:
        image_id = None
        if not self._podman_client.images.exists(f"{self.repository}:{self.tag}") or force_pull:
            logger.info(f"Image {self.repository}:{self.tag} not found, pulling.")

        else:
            logger.debug(f"Image {self.repository}:{self.tag} found locally, skipping pull.")
            image_id = self._podman_client.images.get(f"{self.repository}:{self.tag}").id
        return image_id

    def get_logs(self):
        if self.__container_handle is None:
            return [""]
        return [line.strip() for line in self.__container_handle.logs(stderr=True, stdout=True)]

    def update(self) -> bool:
        # If we don't have a handle, we cannot update.
        if self.__container_handle is None:
            return False

        # Pull the image
        self.get_image_id(True)
        self.__container_handle.restart()

    def create(self) -> bool:
        self.stop_container()
        image_id = self.get_image_id()
        if image_id is None:
            logger.error("Error getting image.")
            return False
        self.__container_handle = self._podman_client.containers.create(image=image_id, command=self.command,
                                                                        name=self.managed_container_name, **self.props)
        logger.info("Starting Container.")
        self.__container_handle.start()
        self.wait(["running", "exited"])
        if self.__container_handle.status == "running":
            logger.info("Container Started.")
            return True
        else:
            logger.error("Error starting container.")
            for line in self.get_logs():
                logger.error(line)
            self.stop_container()
            return False

meta_manager = MetaContainerManager(pc, "meta")

# if __name__ == '__main__':

    # mower_container = ContainerManager(pc, "open-mower", "ghcr.io/clemenselflein/open_mower_ros", "latest", [], {
    #     "mounts": [{
    #         "type": "bind",
    #         "source": "/home/clemens/mower_config.sh",
    #         "target": "/config/mower_config.sh"
    #     }]
    # })
    # mower_container.update()
    # mower_container.create()

    # for c in pc.containers.list(all=True):
    #     logger.info(f"removing and stopping {c.name}")
    #     c.stop(ignore=True)
    #     c.wait(condition=["stopped", "exited"])
    #     c.remove()

    # version = podman_client.version()
    # print("Release: ", version["Version"])
    # print("Compatible API: ", version["ApiVersion"])
    # print("Podman API: ", version["Components"][0]["Details"]["APIVersion"], "\n")

    # get all images
    # print("images:")
    # for image in podman_client.images.list():
    #     print(image, image.id, "\n")
    #     repo, id = image.attrs.get("RepoTags")[0].split(":")
    #     podman_client.images.pull(repo, id)

    # images = podman_client.images.list()
    # print("pulling image")
    # image_id = ""
    # for line in podman_client.images.pull("ghcr.io/clemenselflein/open_mower_ros", "latest", stream=True):
    #     d = json.loads(line)
    #     if "stream" in d:
    #         print(d["stream"])
    #     elif "images" in d:
    #         image_id = d["id"]
    # print("image pulled")

    # repository = "ghcr.io/clemenselflein/open_mower_ros"
    # tag = "releases-edge"
    #
    # image_id = None
    # if not podman_client.images.exists(f"{repository}:{tag}"):
    #     print(f"image {repository}:{tag} not found locally, pulling.")
    #     for line in podman_client.images.pull("ghcr.io/clemenselflein/open_mower_ros", "latest", stream=True):
    #         d = json.loads(line)
    #         if "stream" in d:
    #             print(d["stream"])
    #         elif "images" in d:
    #             image_id = d["id"]
    #     print("image pulled")
    # else:
    #     print(f"image {repository}:{tag} found locally.")
    #     image_id = podman_client.images.get(f"{repository}:{tag}").id
    #
    # raise Exception(f"ID: {image_id}")
    # container = podman_client.containers.create(image=image_id, command=["asdf"], restart_policy={"Name": "always"},
    #                                             ports={"8080": "8080"})
    # print("container created")
    # container.start()
    # container.wait(condition="running")
    # print("container running")
    #
    # print("containers:")
    # # find all containers
    # for container in podman_client.containers.list():
    #     # container.restart()
    #     logs = container.logs()
    #     for line in container.logs():
    #         print(line)
    #
    # print(json.dumps(podman_client.df(), indent=4))
#
@app.post("/update-self")
async def update_self():
    meta_manager.update()
    return {"message": "Hello World"}

@app.get("/version")
async def get_version():
    return "v2"

#
#
# @app.get("/hello/{name}")
# async def say_hello(name: str):
#     return {"message": f"Hello {name}"}
#
#
# @app.get("/images")
# async def get_images():
#     version = podman_client.version()
#     print("Release: ", version["Version"])
#     print("Compatible API: ", version["ApiVersion"])
#     print("Podman API: ", version["Components"][0]["Details"]["APIVersion"], "\n")
#
#     # get all images
#     # print("images:")
#     # for image in podman_client.images.list():
#     #     print(image, image.id, "\n")
#     #     repo, id = image.attrs.get("RepoTags")[0].split(":")
#     #     podman_client.images.pull(repo, id)
#
#     images = podman_client.images.list()
#     print("pulling image")
#     image = podman_client.images.pull("ghcr.io/clemenselflein", "open_mower_ros:latest")
#     print("image pulled")
#     container = podman_client.containers.create(image=image.id, command=["asdf"], restart_policy={"Name": "always"}, ports={"8080":"8080"})
#     print("container created")
#     container.start()
#     container.wait(condition="running")
#     print("container running")
#
#     print("containers:")
#     # find all containers
#     for container in podman_client.containers.list():
#         # container.restart()
#         logs = container.logs()
#         for line in container.logs():
#             print(line)
#
#     print(json.dumps(podman_client.df(), indent=4))
#     return "OK"
