import { ReactNode, useState } from "react";
import MUICard from "@mui/material/Card";
import CardContent from "@mui/material/CardContent";
import Typography from "@mui/material/Typography";
import {
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  IconButton,
  Stack,
  useTheme,
} from "@mui/material";
import { Info } from "@mui/icons-material";
import Button from "@mui/material/Button";

export interface InfoCardProps {
  title: string;
  content: string;
  icon?: ReactNode;
  warning?: boolean;
  infoText?: string;
}

export function InfoCard(props: InfoCardProps) {
  const theme = useTheme();
  const [open, setOpen] = useState(false);
  const infoDialog = (
    <Dialog open={open}>
      <DialogContent>
        <DialogContentText>{props.infoText}</DialogContentText>
      </DialogContent>
      <DialogActions>
        <Button onClick={() => setOpen(false)}>Close</Button>
      </DialogActions>
    </Dialog>
  );
  return (
    <MUICard
      variant="outlined"
      style={props.warning ? { borderColor: theme.palette.warning.main } : {}}
    >
      {infoDialog}
      <CardContent>
        <Stack alignItems="center" direction="row" gap={1}>
          <Typography
            sx={{ fontSize: 14 }}
            color="text.secondary"
            marginTop={"1px"}
            flex={"auto"}
          >
            {props.title}
          </Typography>
          {props.infoText && (
            <IconButton onClick={() => setOpen(true)}>
              <Info fontSize={"small"} />
            </IconButton>
          )}
        </Stack>
        {props.icon ? (
          <Stack alignItems="center" direction="row" gap={1}>
            {props.icon}
            <Typography variant="h5" component="div">
              {props.content}
            </Typography>
          </Stack>
        ) : (
          <Typography variant="h5" component="div">
            {props.content}
          </Typography>
        )}
      </CardContent>
    </MUICard>
  );
}
