import { after, before, test } from "node:test";
import assert from "node:assert/strict";
import { createServer } from "vite";

let server;
let api;
let bookingsApi;
let previousWindow;
let previousStorage;

before(async () => {
  previousWindow = globalThis.window;
  previousStorage = globalThis.localStorage;
  globalThis.window = { location: { protocol: "http:", host: "localhost:5173" } };
  globalThis.localStorage = { getItem: () => null };
  server = await createServer({ configFile: false, server: { middlewareMode: true }, appType: "custom" });
  ({ api, bookingsApi } = await server.ssrLoadModule("/src/api.js"));
});

after(async () => {
  await server?.close();
  globalThis.window = previousWindow;
  globalThis.localStorage = previousStorage;
});

test("review sends JSON both with and without a comment", async () => {
  const requests = [];
  api.defaults.adapter = async (config) => {
    requests.push({ data: config.data, contentType: config.headers.getContentType() });
    return { data: {}, status: 200, statusText: "OK", headers: {}, config };
  };
  await bookingsApi.review(1, null);
  await bookingsApi.review(1, "Please revise");
  assert.deepEqual(requests, [
    { data: "{}", contentType: "application/json" },
    { data: '{"comment":"Please revise"}', contentType: "application/json" },
  ]);
});
