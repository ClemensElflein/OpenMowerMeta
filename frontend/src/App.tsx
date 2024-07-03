import { useState } from "react";
import "./App.css";
import { MainLayout } from "./components/layout/MainLayout.tsx";
import InboxIcon from "@mui/icons-material/MoveToInbox";
import {
  matchPath,
  Outlet,
  useLocation,
  useMatch,
  useNavigate,
} from "react-router-dom";
import { OpenMowerConfigurationPage } from "./pages/app/OpenMowerConfiguration.tsx";
import { Terminal } from "@mui/icons-material";
import { MetaContainerConfigurationPage } from "./pages/app/MetaContainerConfiguration.tsx";

export const AppNavigation = [
  {
    text: "Open Mower Container",
    path: "open-mower",
    element: <OpenMowerConfigurationPage />,
    icon: Terminal,
  },
  {
    text: "Settings",
    path: "meta-container",
    element: <MetaContainerConfigurationPage />,
    icon: InboxIcon,
  },
  {
    text: "Open Mower App",
    path: "open-mower2",
    element: <div>nav</div>,
    icon: InboxIcon,
  },
];

export function App() {
  const location = useLocation();
  let currentIndex = AppNavigation.findIndex((item) =>
    matchPath("/app/" + item.path, location.pathname),
  );
  if (currentIndex < 0) currentIndex = 0;

  const navigate = useNavigate();

  return (
    <MainLayout
      drawerItems={AppNavigation}
      currentIndex={currentIndex}
      onSelect={(newIndex) => {
        navigate("/app/" + AppNavigation[newIndex].path);
      }}
      title={AppNavigation[currentIndex].text ?? "NONE"}
    >
      <Outlet />
    </MainLayout>
  );
}

export default App;
