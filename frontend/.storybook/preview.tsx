import type { Preview } from "@storybook/react";
import "../src/index.css";
import "@fontsource/roboto/300.css";
import "@fontsource/roboto/400.css";
import "@fontsource/roboto/500.css";
import "@fontsource/roboto/700.css";
import appTheme from "../src/theme";
import { ThemeProvider } from "@mui/material";

const preview: Preview = {
  parameters: {
    controls: {
      matchers: {
        color: /(background|color)$/i,
        date: /Date$/i,
      },
    },
  },
  decorators: [
    (Story) => (
      <ThemeProvider theme={appTheme}>
        <Story />
      </ThemeProvider>
    ),
  ],
};

export default preview;
