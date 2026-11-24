import { test } from "node:test";
import assert from "node:assert/strict";
import { gradleArgs, logcatArgs, maskSensitive, tail } from "./policy.js";

test("gradle args for module task", () => {
  assert.deepEqual(gradleArgs("test", ":app"), [":app:test", "--console=plain", "--no-daemon"]);
});

test("rejects injection in module path", () => {
  assert.throws(() => gradleArgs("test", ":app; rm -rf /"));
  assert.throws(() => gradleArgs("test", "app && curl evil"));
});

test("rejects tasks outside allow-list", () => {
  // @ts-expect-error — celowo niedozwolony task
  assert.throws(() => gradleArgs("publishRelease"));
});

test("logcat args with tag filter", () => {
  assert.deepEqual(logcatArgs(50, "ClaimRepo", "W"), ["logcat", "-d", "-t", "50", "ClaimRepo:W", "*:S"]);
});

test("logcat validates input", () => {
  assert.throws(() => logcatArgs(10_000));
  assert.throws(() => logcatArgs(10, "tag $(whoami)"));
});

test("masks personal data in logs", () => {
  const log = "D/Claim: user jan.kowalski@example.com pesel 44051401359 tel 600 700 800 polisa POL-2024-00012345 Authorization: Bearer abcdefghijklmnopqrstuvwxyz";
  const masked = maskSensitive(log);
  for (const raw of ["jan.kowalski@example.com", "44051401359", "600 700 800", "POL-2024-00012345", "abcdefghijklmnop"]) {
    assert.ok(!masked.includes(raw), `${raw} leaked: ${masked}`);
  }
});

test("does not mask numbers with invalid PESEL checksum", () => {
  assert.equal(maskSensitive("build 12345678901"), "build 12345678901");
});

test("tail keeps the end of long output", () => {
  const out = Array.from({ length: 300 }, (_, i) => `l${i}`).join("\n");
  const t = tail(out, 10);
  assert.ok(t.startsWith("…(290 lines omitted)"));
  assert.ok(t.endsWith("l299"));
});
