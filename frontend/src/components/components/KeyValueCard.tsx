import { ReactNode } from "react";

export interface KeyValueCardProps {
  children: ReactNode;
}

export function KeyValueCard({ children }: KeyValueCardProps) {
  return <dl className="divide-y divide-gray-100">{children}</dl>;
}
