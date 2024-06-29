import type { Meta, StoryObj } from "@storybook/react";
import * as React from "react";
import { fn } from "@storybook/test";
import { Drawer } from "../Drawer.tsx";
import Button from "@mui/material/Button";
import { useArgs } from "@storybook/preview-api";

// More on how to set up stories at: https://storybook.js.org/docs/writing-stories#default-export
const meta = {
  title: "Custom/Drawer/Drawer",
  component: Drawer,
  parameters: {
    // Optional parameter to center the component in the Canvas. More info: https://storybook.js.org/docs/configure/story-layout
    layout: "centered",
  },
  // This component will have an automatically generated Autodocs entry: https://storybook.js.org/docs/writing-docs/autodocs
  tags: ["autodocs"],
  // More on argTypes: https://storybook.js.org/docs/api/argtypes
  argTypes: {
    open: { control: "boolean" },
  },
  // Use `fn` to spy on the onClick arg, which will appear in the actions panel once invoked: https://storybook.js.org/docs/essentials/actions#action-args
  args: {
    open: true,
    onClose: fn,
  },
} satisfies Meta<typeof Drawer>;

export default meta;
type Story = StoryObj<typeof meta>;

// More on writing stories with args: https://storybook.js.org/docs/writing-stories/args
export const Primary: Story = {
  render() {
    const [{ open, onClose }, updateArgs] = useArgs();

    const toggleDrawer = (newOpen: boolean) => () => {
      updateArgs({ open: newOpen });
    };

    return (
      <div>
        <Button onClick={toggleDrawer(true)}>Open drawer</Button>
        <Drawer open={open} onClose={onClose}></Drawer>
      </div>
    );
  },
};
