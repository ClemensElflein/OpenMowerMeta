import type { Meta, StoryObj } from "@storybook/react";
import { fn } from "@storybook/test";
import { DrawerContent } from "../DrawerContent.tsx";
import InboxIcon from "@mui/icons-material/MoveToInbox";

const items = [
  { text: "Dashboard", icon: InboxIcon, badge: "5" },
  { text: "Team", icon: InboxIcon, badge: "text" },
  { text: "Projects", icon: InboxIcon, current: false },
  { text: "Calendar", icon: InboxIcon, current: true },
  { text: "Documents", icon: InboxIcon, current: false },
  { text: "Reports", icon: InboxIcon, current: false },
];

// More on how to set up stories at: https://storybook.js.org/docs/writing-stories#default-export
const meta = {
  title: "Custom/Drawer/DrawerContent",
  component: DrawerContent,
  parameters: {
    // Optional parameter to center the component in the Canvas. More info: https://storybook.js.org/docs/configure/story-layout
    layout: "centered",
  },
  // This component will have an automatically generated Autodocs entry: https://storybook.js.org/docs/writing-docs/autodocs
  tags: ["autodocs"],
  // More on argTypes: https://storybook.js.org/docs/api/argtypes
  argTypes: {},
  // Use `fn` to spy on the onClick arg, which will appear in the actions panel once invoked: https://storybook.js.org/docs/essentials/actions#action-args
  args: {
    currentIndex: 0,
    onSelect: fn(),
    items: items,
  },
} satisfies Meta<typeof DrawerContent>;

export default meta;
type Story = StoryObj<typeof meta>;

// More on writing stories with args: https://storybook.js.org/docs/writing-stories/args
export const Primary: Story = {
  args: {},
};
