import logging
import json
from typing import Optional

import podman.domain.containers
from podman import PodmanClient



class ContainerManagerBase:
    """
    Base class for managing containers using Podman.
    This class connects to Podman, looks for a container with name `managed_container_name`
    and gets a handle to it, if exists.

    Args:
        podman_client (PodmanClient): The Podman client object.
        managed_container_name (str): The name of the container being managed.
        logger (logging.Logger): The logger object for logging messages.

    Attributes:
        logger (logging.Logger): The logger object for logging messages.
        _podman_client (PodmanClient): The Podman client object.
        managed_container_name (str): The name of the container being managed.
        _container_handle (podman.domain.containers.Container | None): The handle to the managed container, if it exists.

    """

    def __init__(self, podman_client: PodmanClient, managed_container_name: str, logger: logging.Logger):
        self.logger = logger
        self.logger = logging.getLogger(self.__class__.__name__)
        self._podman_client = podman_client
        self.managed_container_name = managed_container_name
        self._container_handle: podman.domain.containers.Container | None = None
        # Check, if a container already exists, if so, fetch the handle.
        for container in self._podman_client.containers.list(all=True, filters={"name": self.managed_container_name}):
            self._container_handle = container
            self.logger.info(f"Found container: {container.name}")
            break


    def pull_image(self, repository, tag) -> str | None:
        """
        :param repository: The name of the repository from which to pull the image.
        :param tag: The tag of the image to pull.
        :return: The image ID of the pulled image, or None if the image failed to be pulled.

        This method is used to pull an image from a docker repository.

        Example usage:
        ```
        image_id = pull_image("myrepository", "latest")
        if image_id:
            print("Image pulled successfully.")
        else:
            print("Failed to pull the image.")
        ```
        """
        self.logger.info(f"Pulling Image {repository}:{tag}")
        image_id = None
        for line in self._podman_client.images.pull(repository, tag, stream=True):
            # Stream is JSON dicts, decode
            d = json.loads(line)
            if "stream" in d:
                self.logger.debug(d["stream"])
            elif "images" in d:
                image_id = d["id"]
        self.logger.info(f"Image {repository}:{tag} pulled successfully.")
        return image_id

    def wait(self, conditions: [str]):
        """
        :param conditions: A list of conditions that the method waits for.
        :return: None
        """
        if self._container_handle is None:
            return
        while True:
            self._container_handle.wait(condition=conditions, interval="1000ms")
            self._container_handle.reload()
            if self._container_handle.status in conditions:
                break
            self.logger.info(
                f"Container status is {self._container_handle.status}, expected: {conditions} waiting some more."
            )

    def stream_logs(self):
        """
        Stream Docker container logs.

        :return: Docker container logs stream or None, if the container is not active.
        """
        if self._container_handle is None:
            return None
        return self._container_handle.logs(stderr=True, stdout=True, stream=True, follow=True)
