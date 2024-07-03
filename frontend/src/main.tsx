import React from "react";
import ReactDOM from "react-dom/client";
import App, { AppNavigation } from "./App.tsx";
import "./index.css";
import "@fontsource/roboto/300.css";
import "@fontsource/roboto/400.css";
import "@fontsource/roboto/500.css";
import "@fontsource/roboto/700.css";

import TimeAgo from "javascript-time-ago";

import en from "javascript-time-ago/locale/en";
import {
  createBrowserRouter,
  Navigate,
  RouterProvider,
} from "react-router-dom";
import { ErrorPage } from "./pages/ErrorPage.tsx";
import appTheme from "./theme.tsx";
import { ThemeProvider } from "@mui/material";

TimeAgo.addDefaultLocale(en);
const router = createBrowserRouter([
  {
    path: "/",
    errorElement: <ErrorPage />,
    element: <Navigate to={"/app/open-mower"} />,
  },
  {
    path: "app",
    element: <App />,
    id: "app",
    children: AppNavigation,
    errorElement: <ErrorPage />,
  },
]);
ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <ThemeProvider theme={appTheme}>
      <RouterProvider router={router} />
    </ThemeProvider>
  </React.StrictMode>,
);
