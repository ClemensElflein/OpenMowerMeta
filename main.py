import asyncio
import json
import logging
import sys
import threading
import time
from contextlib import asynccontextmanager

from aioreactive import AsyncAnonymousObserver
from aioreactive.subscription import subscribe_async
from fastapi import FastAPI, WebSocket
from fastapi.middleware.cors import CORSMiddleware
from podman import PodmanClient
from ContainerManager import ContainerManager
from MetaContainerManager import MetaContainerManager

logging.basicConfig(level=logging.INFO, format='%(asctime)s -%(levelname)s -on line: %(lineno)d -%(message)s')

pc = PodmanClient(base_url="unix:///run/user/1000/podman/podman.sock")

logger = logging.getLogger(__name__)
handler = logging.StreamHandler()
handler.setLevel(logging.DEBUG)
handler.setStream(sys.stdout)
logger.addHandler(handler)

logger.setLevel(logging.DEBUG)

meta_manager = MetaContainerManager(pc, "meta", logger)
mower_container_manager = ContainerManager(
    pc, "open-mower", "ghcr.io/clemenselflein/open_mower_ros", "latest", [],
    {
        "mounts": [{
            "type": "bind",
            "source": "/home/clemens/mower_config.sh",
            "target": "/config/mower_config.sh"
        }]
    }, logger
)

app = FastAPI()
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.post("/update-self")
async def update_self():
    meta_manager.update()
    return "OK"


@app.get("/container/status")
async def get_container_status():
    return mower_container_manager.state_observable.value


@app.post("/container/action")
def post_container_action(action: str):
    print(action)
    if action == "start":
        mower_container_manager.create()
    elif action == "stop":
        mower_container_manager.stop_container()
    elif action == "pull":
        mower_container_manager.update()
    return mower_container_manager.status()


@app.websocket("/ws/container/status")
async def websocket_endpoint(websocket: WebSocket):
    print("new status connection")
    await websocket.accept()
    await websocket.send_json({"yo": True})
    mower_container_manager.state_observable.subscribe()
    subscription = subscribe_async(mower_container_manager.state_observable)

    disposable = await subscribe_async(mower_container_manager.state_observable, AsyncAnonymousObserver(lambda value: websocket.send_json(value)))
    # version_subscription = mower_container_manager.version_observable.subscribe(lambda value: websocket.send_text({"image_version":value}))
    try:
        while True:
            # need to send something in order to check if the websocket is still alive
            # see: https://github.com/tiangolo/fastapi/discussions/9031
            try:
                await asyncio.wait_for(
                    websocket.receive(), 1.0
                )
            except asyncio.TimeoutError:
                pass
    except Exception as e:
        print("websocket exception", e)
        pass
    state_subscription.dispose()
    # version_subscription.dispose()


@app.websocket("/ws/container/logs")
async def websocket_endpoint(websocket: WebSocket):
    await websocket.accept()

    await websocket.send_text("Log Websocket Connected.")

    # Keep a buffer of lines before sending out in order to not spam the websocket
    lines: [str] = []

    # Podman steam is not asyncio, so we need to run in separate executor
    def stream_data(cancel_event: threading.Event):
        while not cancel_event.is_set():
            gen = mower_container_manager.stream_logs()
            if gen is not None:
                # Stream the lines returned from the generator, cancel on event
                for line in gen:
                    lines.append(line.decode("utf-8"))
                    if cancel_event.is_set():
                        break
                logger.debug("closing generator")
                gen.close()
            if not cancel_event.is_set():
                # Generator stopped, but event is not set - retry after some time
                logger.debug("stream ended, but not stopped; retrying")
                time.sleep(1)

    # A flag to stop the podman stream
    stream_cancel_event = threading.Event()
    loop = asyncio.get_event_loop().run_in_executor(None, stream_data, stream_cancel_event)

    # Also stream the manager's logs, so that we can see image pulls etc
    class Handler(logging.Handler):
        def __init__(self):
            logging.Handler.__init__(self)

        def emit(self, record):
            lines.append(f"[GUI] {self.format(record)}")

    stream_handler = Handler()
    stream_handler.setLevel(logging.INFO)
    mower_container_manager.logger.addHandler(stream_handler)
    try:
        while True:
            # Limit update rate to the websocket
            await asyncio.sleep(0.1)
            # need to send something in order to check if the websocket is still alive
            # see: https://github.com/tiangolo/fastapi/discussions/9031
            try:
                await asyncio.wait_for(
                    websocket.receive(), 0.1
                )
            except asyncio.TimeoutError:
                pass
            if len(lines) > 0:
                await websocket.send_text(str.join("\r\n", lines))
                lines.clear()
    except Exception as e:
        logger.debug(f"Websocket exception: {e}")
        logger.removeHandler(stream_handler)
        stream_cancel_event.set()
        # Wait for the podman stream to finish
        await loop
        logger.debug("WS stream done")
