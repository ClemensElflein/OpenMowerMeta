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
import { InfoCard } from "./InfoCard.tsx";
import { ContainerExecutionState } from "../../datatypes/ContainerExecutionState.tsx";

function format(state: ContainerExecutionState): [ReactNode, string] {
  let displayIcon;
  let displayText;

  switch (state) {
    case ContainerExecutionState.Unknown:
      displayIcon = <QuestionMarkTwoTone color={"warning"} />;
      displayText = "Unknown";
      break;
    case ContainerExecutionState.Error:
      displayIcon = <NewReleasesIcon color={"error"} />;
      displayText = "Error";
      break;
    case ContainerExecutionState.NoContainer:
      displayIcon = <Cancel color={"error"} />;
      displayText = "No Container";
      break;
    case ContainerExecutionState.Created:
      displayIcon = <Cancel color={"error"} />;
      displayText = "Created";
      break;
    case ContainerExecutionState.Exited:
      displayIcon = <Cancel color={"error"} />;
      displayText = "Exited";
      break;
    case ContainerExecutionState.Starting:
      displayIcon = <HourglassEmptyIcon color={"success"} />;
      displayText = "Starting";
      break;
    case ContainerExecutionState.Restarting:
      displayIcon = <LoopIcon color={"warning"} />;
      displayText = "Restarting";
      break;
    case ContainerExecutionState.Running:
      displayIcon = <CheckCircleIcon color={"success"} />;
      displayText = "Running";
      break;
    case ContainerExecutionState.Dead:
      displayIcon = <Cancel color={"error"} />;
      displayText = "Dead";
      break;
    case ContainerExecutionState.Paused:
      displayIcon = <PauseCircleOutlineIcon color={"warning"} />;
      displayText = "Paused";
      break;
    case ContainerExecutionState.Stopping:
      displayIcon = <HourglassEmptyIcon color={"warning"} />;
      displayText = "Stopping";
      break;
    case ContainerExecutionState.Pulling:
      displayIcon = <DownloadingTwoTone color={"primary"} />;
      displayText = "Pulling";
      break;
    default:
      throw new Error("Invalid state");
  }

  return [displayIcon, displayText];
}

export interface ContainerControlCardProps {
  executionState: ContainerExecutionState;
}

export function ContainerStatusCard(props: ContainerControlCardProps) {
  const [displayIcon, displayText] = format(props.executionState);
  return (
    <InfoCard
      title={"Container Status"}
      content={displayText}
      icon={displayIcon}
    />
  );
}
