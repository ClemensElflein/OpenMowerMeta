import React from "react";

function classNames(...classes: string[]) {
  return classes.filter(Boolean).join(" ");
}

export interface DrawerItemProps {
  text: string;
  current?: boolean;
  icon?: React.ElementType;
  onClick?: () => void;
  badge?: string;
}

export function DrawerItem({
  text,
  current,
  icon: Icon,
  badge,
  onClick,
}: DrawerItemProps) {
  return (
    <li key={text}>
      <a
        href={"#"}
        onClick={(e) => {
          e.preventDefault();
          onClick && onClick();
        }}
        className={classNames(
          current
            ? "bg-gray-50 text-indigo-600"
            : "text-gray-700 hover:bg-gray-50 hover:text-indigo-600",
          "group flex gap-x-3 rounded-md p-2 text-sm font-semibold leading-6",
        )}
      >
        {Icon && (
          <Icon
            className={classNames(
              current
                ? "text-indigo-600"
                : "text-gray-400 group-hover:text-indigo-600",
              "h-6 w-6 shrink-0",
            )}
            aria-hidden="true"
          />
        )}
        {text}
        {badge ? (
          <span
            className="ml-auto w-9 min-w-max whitespace-nowrap rounded-full bg-white px-2.5 py-0.5 text-center text-xs font-medium leading-5 text-gray-600 ring-1 ring-inset ring-gray-200"
            aria-hidden="true"
          >
            {badge}
          </span>
        ) : null}
      </a>
    </li>
  );
}
