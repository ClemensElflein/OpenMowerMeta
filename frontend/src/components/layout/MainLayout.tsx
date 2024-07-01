import { ReactNode, useState } from "react";

import MenuIcon from "@mui/icons-material/Menu";
import { Drawer } from "../drawer/Drawer.tsx";
import { DrawerContent } from "../drawer/DrawerContent.tsx";
import { DrawerItemProps } from "../drawer/DrawerItem.tsx";

export interface MainLayoutProps {
  drawerItems: DrawerItemProps[];
  currentIndex: number;
  onSelect: (index: number) => void;
  title: string;
  children: ReactNode;
}

export function MainLayout({
  title,
  currentIndex,
  onSelect,
  drawerItems,
  children,
}: MainLayoutProps) {
  const [sidebarOpen, setSidebarOpen] = useState(false);

  return (
    <>
      <div>
        <Drawer
          open={sidebarOpen}
          onClose={() => setSidebarOpen(false)}
          items={drawerItems}
          currentIndex={currentIndex}
          onSelect={(index) => {
            onSelect(index);
            setSidebarOpen(false);
          }}
        />

        {/* Static sidebar for desktop */}
        <div className="hidden lg:fixed lg:inset-y-0 lg:z-50 lg:flex lg:w-72 lg:flex-col">
          <DrawerContent
            items={drawerItems}
            currentIndex={currentIndex}
            onSelect={onSelect}
          />
        </div>

        <div className="sticky top-0 z-40 flex items-center gap-x-6 bg-white px-4 py-4 shadow-sm sm:px-6 lg:hidden">
          <button
            type="button"
            className="-m-2.5 p-2.5 text-gray-700 lg:hidden"
            onClick={() => setSidebarOpen(true)}
          >
            <span className="sr-only">Open sidebar</span>
            <MenuIcon className="h-6 w-6" aria-hidden="true" />
          </button>
          <div className="flex-1 text-sm font-semibold leading-6 text-gray-900">
            {title}
          </div>
        </div>

        <main className="py-10 lg:pl-72 bg-gray-100 min-h-full">
          <div className="px-4 sm:px-6 lg:px-8">{children}</div>
        </main>
      </div>
    </>
  );
}
