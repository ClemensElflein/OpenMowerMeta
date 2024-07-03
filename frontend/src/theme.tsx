import { createTheme } from "@mui/material";
import colors from "tailwindcss/colors";

const theme = createTheme({
  typography: {
    button: {
      textTransform: "none",
    },
  },
  palette: {
    primary: {
      main: colors.indigo["800"],
    },
    secondary: {
      main: colors.indigo["500"],
    },
    success: {
      main: colors.green["700"],
    },
    warning: {
      main: colors.amber["600"],
    },
    error: {
      main: colors.red["600"],
    },
  },
});

export default theme;
