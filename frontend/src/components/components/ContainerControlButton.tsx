import { ReactNode } from "react";
import { PlayCircle, StopCircle } from "@mui/icons-material";
import { ContainerExecutionState } from "../../datatypes/ContainerExecutionState.tsx";
import Button from "@mui/material/Button";
import HourglassEmptyIcon from "@mui/icons-material/HourglassEmpty";

export enum Action {
  START,
  STOP,
}

function getAction(state: ContainerExecutionState): Action | null {
  switch (state) {
    case ContainerExecutionState.Unknown:
    case ContainerExecutionState.Error:
    case ContainerExecutionState.Created:
    case ContainerExecutionState.Dead:
    case ContainerExecutionState.Paused:
    case ContainerExecutionState.Exited:
    case ContainerExecutionState.NoContainer:
      return Action.START;
    case ContainerExecutionState.Pulling:
    case ContainerExecutionState.Stopping:
    case ContainerExecutionState.Starting:
    case ContainerExecutionState.Restarting:
      return null;
    case ContainerExecutionState.Running:
      return Action.STOP;
    default:
      throw new Error("Invalid state");
  }
}

export interface ContainerControlButtonProps {
  executionState: ContainerExecutionState;
  onAction: (action: Action) => void;
}

export function ContainerControlButton(props: ContainerControlButtonProps) {
  const action = getAction(props.executionState);
  let text: string;
  let enabled: boolean;
  let icon: ReactNode;
  let color: "error" | "success" | "primary";
  switch (action) {
    case Action.STOP:
      text = "Stop Container";
      enabled = true;
      icon = <StopCircle />;
      color = "error";
      break;
    case Action.START:
      text = "Start Container";
      enabled = true;
      icon = <PlayCircle />;
      color = "success";
      break;
    case null:
      text = "Please Wait";
      enabled = false;
      icon = <HourglassEmptyIcon />;
      color = "primary";
      break;
  }
  return (
    <Button
      variant="outlined"
      color={color}
      startIcon={icon}
      disabled={!enabled}
      onClick={() => action != null && props.onAction(action)}
    >
      {text}
    </Button>
  );
}
