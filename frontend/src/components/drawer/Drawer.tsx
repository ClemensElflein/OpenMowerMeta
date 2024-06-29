import { Drawer as MUIDrawer, SvgIcon } from "@mui/material";
import Box from "@mui/material/Box";
import List from "@mui/material/List";
import ListItem from "@mui/material/ListItem";
import ListItemButton from "@mui/material/ListItemButton";
import ListItemIcon from "@mui/material/ListItemIcon";
import ListItemText from "@mui/material/ListItemText";
import Divider from "@mui/material/Divider";
import reactLogo from "../../assets/react.svg";

export interface DrawerProps {
  open: boolean;
  onClose: () => void;
}

const DrawerList = (
  <Box sx={{ width: 250 }} role="presentation">
    <List>
      {["Inbox", "Starred", "Send email", "Drafts"].map((text, index) => (
        <ListItem key={text} disablePadding>
          <ListItemButton>
            <ListItemIcon>
              {/*{index % 2 === 0 ? <InboxIcon /> : <MailIcon />}*/}
            </ListItemIcon>
            <ListItemText primary={text} />
          </ListItemButton>
        </ListItem>
      ))}
    </List>
    <Divider />
    <List>
      {["All mail", "Trash", "Spam"].map((text, index) => (
        <ListItem key={text} disablePadding>
          <ListItemButton>
            <ListItemIcon>
              {/*{index % 2 === 0 ? <InboxIcon /> : <MailIcon />}*/}
            </ListItemIcon>
            <ListItemText primary={text} />
          </ListItemButton>
        </ListItem>
      ))}
    </List>
  </Box>
);

export function Drawer({ open, onClose }: DrawerProps) {
  return (
    <MUIDrawer open={open} onClose={onClose}>
      <img src={reactLogo} className="logo react" alt="React logo" />
      {DrawerList}
    </MUIDrawer>
  );
}
