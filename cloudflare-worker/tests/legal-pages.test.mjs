import test from "node:test";
import assert from "node:assert/strict";
import worker from "../src/index.js";

const env = {};
async function getPage(path) {
  const response = await worker.fetch(new Request("https://image-tools.test" + path), env);
  return { response, body: await response.text() };
}

test("home page advertises the current app release and legal links", async () => {
  const { response, body } = await getPage("/");
  assert.equal(response.status, 200);
  assert.match(response.headers.get("content-type") ?? "", /text\/html/i);
  assert.match(body, /version 1\.18\.0/);
  assert.match(body, /\/privacy/);
  assert.match(body, /\/terms/);
  assert.match(body, /\/support/);
});

test("privacy policy explains local processing, providers and account deletion", async () => {
  const { response, body } = await getPage("/privacy");
  assert.equal(response.status, 200);
  assert.match(body, /Privacy Policy/);
  assert.match(body, /processed locally on your Android device/i);
  assert.match(body, /Firebase Authentication/);
  assert.match(body, /Firebase Analytics/);
  assert.match(body, /Cloudflare/);
  assert.match(body, /PayPal/);
  assert.match(body, /mailto:nexaurenstore@gmail\.com/);
  assert.doesNotMatch(body, /remove backgrounds/i);
});

test("terms page is available and links to support", async () => {
  const { response, body } = await getPage("/terms");
  assert.equal(response.status, 200);
  assert.match(body, /Terms of Service/);
  assert.match(body, /Premium subscriptions/);
  assert.match(body, /mailto:nexaurenstore@gmail\.com/);
});

test("account deletion route gives a private contact path, not a public data request", async () => {
  const { response, body } = await getPage("/support/account-deletion");
  assert.equal(response.status, 200);
  assert.match(body, /Request account deletion/);
  assert.match(body, /contact.*privately/i);
  assert.match(body, /Do not send passwords/i);
  assert.match(body, /mailto:nexaurenstore@gmail\.com/);
});

test("legacy privacy URL remains available", async () => {
  const { response, body } = await getPage("/privacy-policy");
  assert.equal(response.status, 200);
  assert.match(body, /Last updated: October 18, 2026/);
});
