import type { Meta, StoryObj } from "@storybook/react";
import { ContainerControlButton } from "../ContainerControlButton.tsx";
import { ContainerExecutionState } from "../../../datatypes/ContainerExecutionState.tsx";
import { fn } from "@storybook/test";

// More on how to set up stories at: https://storybook.js.org/docs/writing-stories#default-export
const meta = {
  title: "Custom/Components/ContainerControlButton",
  component: ContainerControlButton,
  parameters: {
    // Optional parameter to center the component in the Canvas. More info: https://storybook.js.org/docs/configure/story-layout
    layout: "centered",
  },
  // This component will have an automatically generated Autodocs entry: https://storybook.js.org/docs/writing-docs/autodocs
  tags: ["autodocs"],
  // More on argTypes: https://storybook.js.org/docs/api/argtypes
  argTypes: {
    executionState: {
      control: "select",
      options: Object.values(ContainerExecutionState),
    },
  },
  // Use `fn` to spy on the onClick arg, which will appear in the actions panel once invoked: https://storybook.js.org/docs/essentials/actions#action-args
  args: {
    executionState: ContainerExecutionState.Unknown,
    onAction: fn(),
  },
} satisfies Meta<typeof ContainerControlButton>;

export default meta;
type Story = StoryObj<typeof meta>;

// More on writing stories with args: https://storybook.js.org/docs/writing-stories/args
export const Primary: Story = {
  args: {},
};

export const Unknown: Story = {
  args: {
    executionState: ContainerExecutionState.Unknown,
  },
};

export const Error: Story = {
  args: {
    executionState: ContainerExecutionState.Error,
  },
};

export const Created: Story = {
  args: {
    executionState: ContainerExecutionState.Created,
  },
};

export const Exited: Story = {
  args: {
    executionState: ContainerExecutionState.Exited,
  },
};

export const Starting: Story = {
  args: {
    executionState: ContainerExecutionState.Starting,
  },
};

export const Restarting: Story = {
  args: {
    executionState: ContainerExecutionState.Restarting,
  },
};

export const Running: Story = {
  args: {
    executionState: ContainerExecutionState.Running,
  },
};

export const Dead: Story = {
  args: {
    executionState: ContainerExecutionState.Dead,
  },
};

export const Paused: Story = {
  args: {
    executionState: ContainerExecutionState.Paused,
  },
};

export const Stopping: Story = {
  args: {
    executionState: ContainerExecutionState.Stopping,
  },
};

export const Pulling: Story = {
  args: {
    executionState: ContainerExecutionState.Pulling,
  },
};
export const NoContainer: Story = {
  args: {
    executionState: ContainerExecutionState.NoContainer,
  },
};
