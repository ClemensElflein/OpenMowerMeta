import { useRouteError } from "react-router-dom";

export function ErrorPage() {
  const error = useRouteError() as Error;

  return (
    <>
      {/*
        This example requires updating your template:

        ```
        <html class="h-full">
        <body class="h-full">
        ```
      */}
      <main className="grid min-h-full place-items-center bg-white px-6 py-24 sm:py-32 lg:px-8">
        <div className="text-center">
          <p className="text-base font-semibold text-indigo-600">Error</p>
          <h1 className="mt-4 text-3xl font-bold tracking-tight text-gray-900 sm:text-5xl">
            {error.name ?? "Error"}
          </h1>
          <p className="mt-6 text-base leading-7 text-gray-600">
            {error.message ?? "Unknown Error"}
          </p>
          <p className="mt-6 text-base leading-7 text-gray-600">
            {error.stack}
          </p>
          <div className="mt-10 flex items-center justify-center gap-x-6">
            <a
              href="/public"
              className="rounded-md bg-indigo-600 px-3.5 py-2.5 text-sm font-semibold text-white shadow-sm hover:bg-indigo-500 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-indigo-600"
            >
              Go back home
            </a>
          </div>
        </div>
      </main>
    </>
  );
}
