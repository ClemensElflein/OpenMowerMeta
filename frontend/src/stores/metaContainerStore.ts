import { create } from "zustand";
import { containerApi, rxStomp } from "../api.ts";
import { Subscription } from "rxjs";
import { ContainerExecutionState } from "../datatypes/ContainerExecutionState.tsx";
import { RJSFSchema } from "@rjsf/utils";

export interface ContainerState {
  executionState: ContainerExecutionState;
  runningImage?: string;
  runningImageTag?: string;
  configuredImage?: string;
  configuredImageTag?: string;
  appProperties?: { [key: string]: string };
  startedAt?: string;
}

interface OpenMowerContainerStore {
  state: ContainerState;
  settingsSchema?: RJSFSchema;
  settingsValue?: object;
  subscriptions: Subscription[];
  startContainer: () => Promise<void>;
  stopContainer: () => Promise<void>;
  pullImage: () => Promise<void>;
  loadSettings: () => Promise<void>;
  saveSettings: (data: object) => Promise<void>;
}

export const useMetaContainerStore = create<OpenMowerContainerStore>(
  (set, get) => ({
    state: {
      executionState: ContainerExecutionState.Unknown,
      runningImage: "unknown",
      runningImageTag: "unknown",
      configuredImage: "unknown",
      configuredImageTag: "unknown",
      startedAt: undefined,
      appProperties: {
        "om-version": "unknown",
      },
    },
    subscriptions: [
      rxStomp
        .watch(`/topic/container/open-mower-meta/state`)
        .subscribe((message) =>
          set({ state: JSON.parse(message.body) as ContainerState }),
        ),
    ],

    async loadSettings() {
      await containerApi.getAppSettingsById("open-mower-meta").then((data) =>
        set({
          settingsSchema: JSON.parse(data.data.schema) as RJSFSchema,
          settingsValue: data.data.value,
        }),
      );
    },
    async saveSettings(data: object) {
      await containerApi
        .saveAppSettingsById("open-mower-meta", data)
        .then((data) => set({ settingsValue: data.data }));
    },
    async startContainer() {
      await containerApi
        .executeAction("open-mower-meta", "start")
        .then((data) => set({ state: data.data as ContainerState }));
    },
    async stopContainer() {
      await containerApi
        .executeAction("open-mower-meta", "stop")
        .then((data) => set({ state: data.data as ContainerState }));
    },
    async pullImage() {
      await containerApi
        .executeAction("open-mower-meta", "pull")
        .then((data) => set({ state: data.data as ContainerState }));
    },
  }),
);
