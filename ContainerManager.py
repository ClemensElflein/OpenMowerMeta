import logging
from typing import Dict, List

from podman import PodmanClient

from ContainerManagerBase import ContainerManagerBase


class ContainerManager(ContainerManagerBase):
    """
    ContainerManager - Manage a Podman container

    This class provides functionality to manage a Podman container.

    Attributes:
        podman_client (PodmanClient): A Podman client object.
        managed_container_name (str): The name of the container to be managed.
        repository (str): The repository of the image.
        tag (str): The tag of the image.
        command (List[str]): The command to execute in the container. Empty array for default.
        props (Dict): Additional properties for container creation. See Podman API for info on this.
        logger (logging.Logger): The logger object for logging messages.

    Methods:
        stop_container: Stop and remove the managed container.
        get_image_id: Get the image ID of the Docker image.
        get_logs: Get the logs of the managed container.
        update: Update the managed container by stopping, pulling, and creating a new one.
        create: Create and start the managed container.
        status: Get the status of the managed container.

    """
    def __init__(self, podman_client: PodmanClient, managed_container_name: str, repository: str, tag: str,
                 command: List[str], props: Dict, logger: logging.Logger):
        super().__init__(podman_client, managed_container_name, logger)
        self.repository = repository
        self.tag = tag
        self.command = command
        self.props = props

    def stop_container(self):
        """
        Stop and remove the container, if it exists.

        :return: None
        """
        if self._container_handle is None:
            return
        self.logger.info(f"Stopping and removing container.")
        self._container_handle.stop(ignore=True)
        self.wait(["stopped", "exited", "configured"])

        self.logger.info(f"Container {self.managed_container_name} stopped.")
        self._container_handle.remove()
        self.logger.info(f"Container {self.managed_container_name} removed.")
        self._container_handle = None

    def get_image_id(self) -> str | None:
        """
        Get the ID of the image which should be used for the container.
        If the image is locally found, this will not pull the image. If not, the image will be pulled first.

        :return: The ID of the image if was found or we were able to pull it, else None.
        """
        if not self._podman_client.images.exists(f"{self.repository}:{self.tag}"):
            self.logger.info(f"Image {self.repository}:{self.tag} not found, pulling.")
            return self.pull_image(self.repository, self.tag)
        else:
            self.logger.debug(f"Image {self.repository}:{self.tag} found locally, skipping pull.")
            return self._podman_client.images.get(f"{self.repository}:{self.tag}").id

    def update(self):
        """
        Stops the container if it's running, pulls the image from the repository with the given tag,
        and then creates a new container using the new image.

        :return: None
        """
        # Stop container, if it's running
        self.stop_container()
        # Pull the image
        self.pull_image(self.repository, self.tag)
        # Create new container using the new image.
        self.create()

    def create(self) -> bool:
        """
        Method to create a container using the specified image and properties.

        :return: True if the container is successfully created and started, False otherwise.
        """
        self.stop_container()
        image_id = self.get_image_id()
        if image_id is None:
            self.logger.error("Error getting image.")
            return False
        self._container_handle = self._podman_client.containers.create(image=image_id, command=self.command,
                                                                       name=self.managed_container_name, **self.props)
        self.logger.info("Starting Container.")
        self._container_handle.start()
        self.wait(["running", "exited"])
        if self._container_handle.status == "running":
            self.logger.info("Container Started.")
            return True
        else:
            self.logger.error("Error starting container.")
            for line in self.get_logs():
                self.logger.error(line)
            self.stop_container()
            return False

    def status(self) -> str:
        """
        Returns the status of the container.

        :return: The status of the container. Possible values are:
                 - "no-container" if we don't have a handle to a container at all
                 - the status of the container
        """
        if self._container_handle is None:
            return "no-container"
        self._container_handle.reload()
        return self._container_handle.status
