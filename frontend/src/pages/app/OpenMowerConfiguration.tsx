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
import { useOpenMowerContainerStore } from "@stores/openMowerContainerStore.ts";

export function OpenMowerConfigurationPage() {
  const store = useOpenMowerContainerStore();

  const state = store.state.executionState;

  // Keep track of local edits
  const [formData, setFormData] = useState(store.settingsValue);
  const submitFormRef = createRef<HTMLButtonElement>();
  // If the store provides a new settingsValue, refresh the Form (initially it's null, so default settings will be shown otherwise)
  useEffect(() => setFormData(store.settingsValue), [store.settingsValue]);

  return (
    <div>
      <Typography variant={"h4"} flexGrow={1}>
        Open Mower
      </Typography>
      <Typography variant={"body1"} gutterBottom>
        Use this page to manage the Open Mower container and the basic settings
        for the Open Mower software.
      </Typography>
      <Card className={"mt-6"}>
        <CardHeader
          title={"Container Control"}
          subheader={"Control the Open Mower Container"}
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
              <ContainerControlButton
                executionState={state}
                onAction={(a) => {
                  switch (a) {
                    case Action.STOP:
                      store.stopContainer();
                      break;
                    case Action.START:
                      store.startContainer();
                      break;
                  }
                }}
              />
            </Stack>
          }
        ></CardHeader>
        <KeyValueCard>
          <ContainerStatusKeyValueCardItem executionState={state} />
          <KeyValueCardItem
            id={"image"}
            title={"Image"}
            value={
              <div>
                {store.state.runningImage}
                {store.state.executionState ==
                  ContainerExecutionState.Running &&
                  store.state.runningImage != store.state.configuredImage && (
                    <Typography variant={"body2"} fontWeight={"bold"}>
                      Restart Container to apply changes.
                    </Typography>
                  )}
              </div>
            }
          />
          <KeyValueCardItem
            id={"image-tag"}
            title={"Image Tag"}
            value={
              <div>
                {store.state.runningImageTag}
                {store.state.executionState ==
                  ContainerExecutionState.Running &&
                  store.state.runningImageTag !=
                    store.state.configuredImageTag && (
                    <Typography variant={"body2"} fontWeight={"bold"}>
                      Restart Container to apply changes.
                    </Typography>
                  )}
              </div>
            }
          />
          <KeyValueCardItem
            id={"om-version"}
            title={"Open Mower Version"}
            value={
              (store.state.appProperties &&
                store.state.appProperties["om-version"]) ??
              "unknown"
            }
          />
          <KeyValueCardItem
            id={"runtime"}
            title={"Container Running Since"}
            value={
              store.state.startedAt ? (
                <ReactTimeAgo
                  date={Date.parse(store.state.startedAt)}
                  locale={"en-US"}
                />
              ) : (
                "---"
              )
            }
          />
          <KeyValueCardItem
            id={"environment"}
            title={"Environment"}
            value={
              <Accordion variant={"outlined"}>
                <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                  Display Container Environment
                </AccordionSummary>
                <AccordionDetails>
                  <pre>
                    {store.state.appProperties &&
                      store.state.appProperties["environment"]}
                  </pre>
                </AccordionDetails>
              </Accordion>
            }
          />
        </KeyValueCard>
      </Card>

      <Card className={"mt-6"}>
        <CardHeader
          title={"Settings"}
          subheader={"Setup your Open Mower"}
          style={{ paddingBottom: 0 }}
          action={
            <Stack className={"p-2"} direction={"row"} gap={2}>
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
