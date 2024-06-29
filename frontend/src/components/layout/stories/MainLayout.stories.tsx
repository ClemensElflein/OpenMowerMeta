import type { Meta, StoryObj } from "@storybook/react";
import { MainLayout } from "../MainLayout.tsx";
import { useState } from "react";
import InboxIcon from "@mui/icons-material/MoveToInbox";

// More on how to set up stories at: https://storybook.js.org/docs/writing-stories#default-export
const meta = {
  title: "Custom/Layout/MainLayout",
  component: MainLayout,
  parameters: {
    // Optional parameter to center the component in the Canvas. More info: https://storybook.js.org/docs/configure/story-layout
    layout: "fullscreen",
  },
  // This component will have an automatically generated Autodocs entry: https://storybook.js.org/docs/writing-docs/autodocs
  tags: ["autodocs"],
  // More on argTypes: https://storybook.js.org/docs/api/argtypes
  argTypes: {},
  // Use `fn` to spy on the onClick arg, which will appear in the actions panel once invoked: https://storybook.js.org/docs/essentials/actions#action-args
  args: {
    title: undefined,
    currentIndex: undefined,
    onSelect: undefined,
    drawerItems: [],
    children: <></>,
  },
} satisfies Meta<typeof MainLayout>;

export default meta;
type Story = StoryObj<typeof meta>;

const items = [
  { text: "Dashboard", icon: InboxIcon, badge: "5" },
  { text: "Team", icon: InboxIcon, badge: "text" },
  { text: "Projects", icon: InboxIcon, current: false },
  { text: "Calendar", icon: InboxIcon, current: true },
  { text: "Documents", icon: InboxIcon, current: false },
  { text: "Reports", icon: InboxIcon, current: false },
];

// More on writing stories with args: https://storybook.js.org/docs/writing-stories/args
export const Primary: Story = {
  args: {},
  render() {
    const [currentIndex, setCurrentIndex] = useState(0);

    return (
      <MainLayout
        title={items[currentIndex]?.text ?? "None"}
        currentIndex={currentIndex}
        onSelect={(index) => setCurrentIndex(index)}
        drawerItems={items}
        children={<div>Yo</div>}
      />
    );
  },
};
