import { Drawer as MUIDrawer } from "@mui/material";
import { DrawerContent, DrawerContentProps } from "./DrawerContent.tsx";

export interface DrawerProps extends DrawerContentProps {
  open: boolean;
  onClose: () => void;
}

export function Drawer({ open, onClose, ...rest }: DrawerProps) {
  return (
    <MUIDrawer open={open} onClose={onClose}>
      <DrawerContent {...rest} />
    </MUIDrawer>
  );
}
