import { DrawerItem, DrawerItemProps } from "./DrawerItem.tsx";
import Box from "@mui/material/Box";

export interface DrawerContentProps {
  items: DrawerItemProps[];
  currentIndex: number;
  onSelect: (index: number) => void;
}

export function DrawerContent({
  currentIndex,
  onSelect,
  items,
}: DrawerContentProps) {
  return (
    <Box
      sx={{ width: 300 }}
      className="flex grow flex-col gap-y-5 overflow-y-auto border-r border-gray-200 bg-white px-6"
      role="presentation"
    >
      <div className="flex h-16 shrink-0 items-center">
        <img
          className="h-8 w-auto"
          src="https://tailwindui.com/img/logos/mark.svg?color=indigo&shade=600"
          alt="Your Company"
        />
      </div>
      <nav className="flex flex-1 flex-col">
        <ul role="list" className="flex flex-1 flex-col gap-y-7">
          <li>
            <ul role="list" className="-mx-2 space-y-1">
              {items.map((item, index) => (
                <DrawerItem
                  {...item}
                  current={index == currentIndex}
                  onClick={() => {
                    onSelect(index);
                  }}
                />
              ))}
            </ul>
          </li>

          <li className="-mx-6 mt-auto">
            <a
              href="#"
              className="flex items-center gap-x-4 px-6 py-3 text-sm font-semibold leading-6 text-gray-900 hover:bg-gray-50"
            >
              <img
                className="h-8 w-8 rounded-full bg-gray-50"
                src="https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?ixlib=rb-1.2.1&ixid=eyJhcHBfaWQiOjEyMDd9&auto=format&fit=facearea&facepad=2&w=256&h=256&q=80"
                alt=""
              />
              <span className="sr-only">Your profile</span>
              <span aria-hidden="true">Tom Cook</span>
            </a>
          </li>
        </ul>
      </nav>
    </Box>
  );
}
