import type { Meta, StoryObj } from "@storybook/react";
import { DrawerItem } from "../DrawerItem.tsx";
import InboxIcon from "@mui/icons-material/MoveToInbox";
import { fn } from "@storybook/test";
// More on how to set up stories at: https://storybook.js.org/docs/writing-stories#default-export
const meta = {
  title: "Custom/Drawer/DrawerItem",
  component: DrawerItem,
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
    icon: InboxIcon,
    onClick: fn(),
  },
  decorators: [
    (Story) => (
      <nav className="flex flex-1 flex-col">
        <ul role="list" className="flex flex-1 flex-col gap-y-7">
          <li>
            <ul role="list" className="-mx-2 space-y-1">
              <Story />
            </ul>
          </li>
        </ul>
      </nav>
    ),
  ],
} satisfies Meta<typeof DrawerItem>;

export default meta;
type Story = StoryObj<typeof meta>;

// More on writing stories with args: https://storybook.js.org/docs/writing-stories/args
export const Primary: Story = {
  args: {
    text: "Default Item",
    current: false,
  },
};
export const Selected: Story = {
  args: {
    text: "Selected Item",
    current: true,
  },
};
