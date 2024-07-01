export enum ContainerExecutionState {
  Unknown = "unknown",
  Error = "error",
  Created = "created",
  Exited = "exited",
  Starting = "starting",
  Restarting = "restarting",
  Running = "running",
  Dead = "dead",
  Paused = "paused",
  Stopping = "stopping",
  Pulling = "pulling",
  NoContainer = "no-container",
}
