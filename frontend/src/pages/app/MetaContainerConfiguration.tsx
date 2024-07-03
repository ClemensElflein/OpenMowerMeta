import Typography from "@mui/material/Typography";
import {
  Accordion,
  AccordionDetails,
  AccordionSummary,
  Card,
  CardHeader,
  Stack,
} from "@mui/material";
import Button from "@mui/material/Button";
import {
  Action,
  ContainerControlButton,
} from "@components/components/ContainerControlButton.tsx";
import { KeyValueCard } from "@components/components/KeyValueCard.tsx";
import { ContainerStatusKeyValueCardItem } from "@components/components/ContainerStatusKeyValueCardItem.tsx";
import { KeyValueCardItem } from "@components/components/KeyValueCardItem.tsx";
import { ContainerExecutionState } from "../../datatypes/ContainerExecutionState.tsx";
import ReactTimeAgo from "react-time-ago";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import CardContent from "@mui/material/CardContent";
import Form from "@rjsf/mui";
import validator from "@rjsf/validator-ajv8";
import { IChangeEvent } from "@rjsf/core";
import { createRef, useEffect, useState } from "react";
import { useMetaContainerStore } from "@stores/metaContainerStore.ts";

export function MetaContainerConfigurationPage() {
  const store = useMetaContainerStore();

  const state = store.state.executionState;

  // Keep track of local edits
  const [formData, setFormData] = useState(store.settingsValue);
  const submitFormRef = createRef<HTMLButtonElement>();
  // If the store provides a new settingsValue, refresh the Form (initially it's null, so default settings will be shown otherwise)
  useEffect(() => setFormData(store.settingsValue), [store.settingsValue]);

  return (
    <div>
      <Typography variant={"h4"} flexGrow={1}>
        Settings
      </Typography>
      <Typography variant={"body1"} gutterBottom>
        Use this page to configure the meta container. The page allows you to
        setup the meta container and pull updates for it.
      </Typography>

      <Card className={"mt-6"}>
        <CardHeader
          title={"Meta Container Configuration"}
          subheader={"Setup your Open Mower Meta container."}
          style={{ paddingBottom: 0 }}
          action={
            <Stack className={"p-2"} direction={"row"} gap={2}>
              <Button variant={"outlined"} color={"inherit"}>
                Logs
              </Button>
              <Button variant={"outlined"} color={"inherit"}>
                Settings
              </Button>
              <Button
                variant={"outlined"}
                color={"inherit"}
                onClick={store.pullImage}
              >
                Pull Image
              </Button>
              <Button
                variant={"outlined"}
                color={"primary"}
                onClick={() => submitFormRef.current?.click()}
              >
                Save Settings
              </Button>
            </Stack>
          }
        ></CardHeader>
        <CardContent style={{ paddingTop: 0 }}>
          {store.settingsSchema ? (
            <Form
              schema={store.settingsSchema}
              validator={validator}
              formData={formData}
              action={"#"}
              onSubmit={(e: IChangeEvent) => store.saveSettings(e.formData)}
              onChange={(e) => setFormData(e.formData)}
            >
              <button
                ref={submitFormRef}
                type="submit"
                style={{ display: "none" }}
              />
            </Form>
          ) : (
            <Button onClick={store.loadSettings}>Load Schema</Button>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
