import { ReactNode } from "react";
import { Stack } from "@mui/material";

export interface KeyValueCardItemProps {
  id: string;
  title: string;
  icon?: ReactNode;
  value: ReactNode;
}

export function KeyValueCardItem({
  id,
  title,
  icon,
  value,
}: KeyValueCardItemProps) {
  return (
    <div className="px-4 py-6 sm:grid sm:grid-cols-3 sm:gap-4" key={id}>
      <dt className="text-sm font-medium text-gray-900">{title}</dt>
      <dd className="mt-1 text-sm leading-6 text-gray-700 sm:col-span-2 sm:mt-0">
        {icon ? (
          <Stack alignItems="center" direction="row" gap={1}>
            {icon}
            {value}
          </Stack>
        ) : (
          value
        )}
      </dd>
    </div>
  );
}
