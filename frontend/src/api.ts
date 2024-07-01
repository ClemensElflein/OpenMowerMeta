import { ContainerApi } from "@generated/openapi";
import { RxStomp } from "@stomp/rx-stomp";

export const basePath =
  import.meta.env.MODE === "development" ? "http://localhost:8080/v1" : "/v1";

export const containerApi = new ContainerApi(undefined, basePath);
export const rxStomp = new RxStomp();

function buildBrokerURL() {
  if (import.meta.env.MODE === "development")
    return "ws://localhost:8080/stomp";

  const protocol = window.location.protocol === "https:" ? "wss" : "ws";
  return `${protocol}://${window.location.hostname}:${window.location.port}/stomp`;
}

rxStomp.configure({
  brokerURL: buildBrokerURL(),
});
rxStomp.activate();
