import type { Meta, StoryObj } from "@storybook/react";
import { InfoCard } from "../InfoCard.tsx";
import { Check } from "@mui/icons-material";

// More on how to set up stories at: https://storybook.js.org/docs/writing-stories#default-export
const meta = {
  title: "Custom/Components/InfoCard",
  component: InfoCard,
  parameters: {
    // Optional parameter to center the component in the Canvas. More info: https://storybook.js.org/docs/configure/story-layout
    layout: "centered",
  },
  // This component will have an automatically generated Autodocs entry: https://storybook.js.org/docs/writing-docs/autodocs
  tags: ["autodocs"],
  // More on argTypes: https://storybook.js.org/docs/api/argtypes
  argTypes: {
    title: { control: "text" },
    content: { control: "text" },
    icon: { control: false },
  },
  // Use `fn` to spy on the onClick arg, which will appear in the actions panel once invoked: https://storybook.js.org/docs/essentials/actions#action-args
  args: {
    icon: <Check color={"success"} />,
  },
} satisfies Meta<typeof InfoCard>;

export default meta;
type Story = StoryObj<typeof meta>;

// More on writing stories with args: https://storybook.js.org/docs/writing-stories/args
export const Primary: Story = {
  args: {
    title: "The Title",
    content: "Some Content",
  },
};

export const Warning: Story = {
  args: {
    title: "Warning",
    content: "Some information which is suboptimal",
    warning: true,
    icon: undefined,
  },
};

export const WarningWithInfo: Story = {
  args: {
    title: "Warning",
    content:
      "Some information which is suboptimal. For additional info, click the icon.",
    warning: true,
    infoText: "Here you can put some additional explanation for the user!",
    icon: undefined,
  },
};
