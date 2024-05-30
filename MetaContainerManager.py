import logging

from podman import PodmanClient

from ContainerManagerBase import ContainerManagerBase


class MetaContainerManager(ContainerManagerBase):
    """
    MetaContainerManager is a subclass of ContainerManagerBase and is used to manage the container running THIS software.

    Attributes:
        - podman_client (PodmanClient): The Podman client used to interact with the container engine.
        - managed_container_name (str): The name of the managed container.
        - logger (logging.Logger): The logger used for logging information.
    """
    def __init__(self, podman_client: PodmanClient, managed_container_name: str, logger: logging.Logger):
        super().__init__(podman_client, managed_container_name,logger)

    def update(self) -> bool:
        # Update the meta container, if we don't have a handle, we cannot update
        if self._container_handle is None:
            self.logger.warning("No container handle, cannot update meta container image.")
            return False
        # Fetch image repo and tag from the container handle and pull
        image_repo, tag = self._container_handle.attrs["Image"].split(":")
        self.pull_image(image_repo, tag)
        # Stop the container, systemd will restart it for us.
        self._container_handle.stop()
        return True


