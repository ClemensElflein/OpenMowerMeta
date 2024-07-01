import type { Meta, StoryObj } from "@storybook/react";
import { KeyValueCard } from "../KeyValueCard.tsx";
import { KeyValueCardItem } from "../KeyValueCardItem.tsx";
import Box from "@mui/material/Box";

// More on how to set up stories at: https://storybook.js.org/docs/writing-stories#default-export
const meta = {
  title: "Custom/Components/KeyValueCard",
  component: KeyValueCard,
  parameters: {
    // Optional parameter to center the component in the Canvas. More info: https://storybook.js.org/docs/configure/story-layout
    layout: "centered",
  },
  // This component will have an automatically generated Autodocs entry: https://storybook.js.org/docs/writing-docs/autodocs
  tags: ["autodocs"],
  // More on argTypes: https://storybook.js.org/docs/api/argtypes
  argTypes: {
    children: { control: false },
  },
  // Use `fn` to spy on the onClick arg, which will appear in the actions panel once invoked: https://storybook.js.org/docs/essentials/actions#action-args
  args: {},
  decorators: [
    (Story) => (
      <Box sx={{ width: 1000 }}>
        <Story />
      </Box>
    ),
  ],
} satisfies Meta<typeof KeyValueCard>;

export default meta;
type Story = StoryObj<typeof meta>;

// More on writing stories with args: https://storybook.js.org/docs/writing-stories/args
export const Primary: Story = {
  args: {
    children: (
      <>
        <KeyValueCardItem
          id={"itm1"}
          title={"User Name"}
          value={"Max Mustermann"}
        />
      </>
    ),
  },
};
