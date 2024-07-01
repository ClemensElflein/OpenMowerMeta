import { ReactNode } from "react";
import CheckCircleIcon from "@mui/icons-material/CheckCircle";
import PauseCircleOutlineIcon from "@mui/icons-material/PauseCircleOutline";
import LoopIcon from "@mui/icons-material/Loop";
import NewReleasesIcon from "@mui/icons-material/NewReleases";
import HourglassEmptyIcon from "@mui/icons-material/HourglassEmpty";
import {
  Cancel,
  DownloadingTwoTone,
  QuestionMarkTwoTone,
} from "@mui/icons-material";
import { ContainerExecutionState } from "../../datatypes/ContainerExecutionState.tsx";
import { KeyValueCardItem } from "./KeyValueCardItem.tsx";

function format(state: ContainerExecutionState): [ReactNode, string] {
  let displayIcon;
  let displayText;

  switch (state) {
    case ContainerExecutionState.Unknown:
      displayIcon = (
        <QuestionMarkTwoTone fontSize={"small"} color={"warning"} />
      );
      displayText = "Unknown";
      break;
    case ContainerExecutionState.Error:
      displayIcon = <NewReleasesIcon fontSize={"small"} color={"error"} />;
      displayText = "Error";
      break;
    case ContainerExecutionState.NoContainer:
      displayIcon = <Cancel fontSize={"small"} color={"error"} />;
      displayText = "No Container";
      break;
    case ContainerExecutionState.Created:
      displayIcon = <Cancel fontSize={"small"} color={"error"} />;
      displayText = "Created";
      break;
    case ContainerExecutionState.Exited:
      displayIcon = <Cancel fontSize={"small"} color={"error"} />;
      displayText = "Exited";
      break;
    case ContainerExecutionState.Starting:
      displayIcon = <HourglassEmptyIcon fontSize={"small"} color={"success"} />;
      displayText = "Starting";
      break;
    case ContainerExecutionState.Restarting:
      displayIcon = <LoopIcon fontSize={"small"} color={"warning"} />;
      displayText = "Restarting";
      break;
    case ContainerExecutionState.Running:
      displayIcon = <CheckCircleIcon fontSize={"small"} color={"success"} />;
      displayText = "Running";
      break;
    case ContainerExecutionState.Dead:
      displayIcon = <Cancel fontSize={"small"} color={"error"} />;
      displayText = "Dead";
      break;
    case ContainerExecutionState.Paused:
      displayIcon = (
        <PauseCircleOutlineIcon fontSize={"small"} color={"warning"} />
      );
      displayText = "Paused";
      break;
    case ContainerExecutionState.Stopping:
      displayIcon = <HourglassEmptyIcon fontSize={"small"} color={"warning"} />;
      displayText = "Stopping";
      break;
    case ContainerExecutionState.Pulling:
      displayIcon = <DownloadingTwoTone fontSize={"small"} color={"primary"} />;
      displayText = "Pulling";
      break;
    default:
      throw new Error("Invalid state");
  }

  return [displayIcon, displayText];
}

export interface ContainerStatusKeyValueCardItemProps {
  executionState: ContainerExecutionState;
}

export function ContainerStatusKeyValueCardItem(
  props: ContainerStatusKeyValueCardItemProps,
) {
  const [displayIcon, displayText] = format(props.executionState);
  return (
    <KeyValueCardItem
      id={"container-status"}
      title={"Container Status"}
      value={displayText}
      icon={displayIcon}
    />
  );
}
