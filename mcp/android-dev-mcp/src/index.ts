#!/usr/bin/env node
/**
 * android-dev-mcp — serwer MCP udostępniający agentowi AI bezpieczny, ograniczony
 * zestaw narzędzi deweloperskich Androida: Gradle (lint/test/build), adb devices, logcat.
 *
 * Projekt Androida: zmienna ANDROID_PROJECT_DIR (domyślnie bieżący katalog).
 */
import { execFile } from "node:child_process";
import { existsSync } from "node:fs";
import { join, resolve } from "node:path";
import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { z } from "zod";
import { GRADLE_TASKS, PRIORITIES, gradleArgs, logcatArgs, maskSensitive, tail, type GradleTask } from "./policy.js";

const PROJECT_DIR = resolve(process.env.ANDROID_PROJECT_DIR ?? process.cwd());
const GRADLE_TIMEOUT_MS = Number(process.env.GRADLE_TIMEOUT_MS ?? 10 * 60_000);
const ADB = process.env.ADB_PATH ?? "adb";

type Run = { code: number; output: string };

function run(cmd: string, args: string[], timeoutMs: number): Promise<Run> {
  return new Promise((done) => {
    execFile(cmd, args, { cwd: PROJECT_DIR, timeout: timeoutMs, maxBuffer: 20 * 1024 * 1024 },
      (err, stdout, stderr) => {
        const code = err ? (typeof err.code === "number" ? err.code : 1) : 0;
        const timedOut = err && (err as NodeJS.ErrnoException & { killed?: boolean }).killed;
        done({ code, output: `${stdout}\n${stderr}${timedOut ? "\n[timeout]" : ""}` });
      });
  });
}

const text = (t: string) => ({ content: [{ type: "text" as const, text: t }] });

async function gradle(task: GradleTask, module?: string) {
  let args: string[];
  try {
    args = gradleArgs(task, module);
  } catch (e) {
    return { ...text(`Rejected: ${(e as Error).message}`), isError: true };
  }
  const wrapper = join(PROJECT_DIR, process.platform === "win32" ? "gradlew.bat" : "gradlew");
  if (!existsSync(wrapper)) return text(`Gradle wrapper not found in ${PROJECT_DIR}. Set ANDROID_PROJECT_DIR.`);
  const { code, output } = await run(wrapper, args, GRADLE_TIMEOUT_MS);
  return text(`$ ./gradlew ${args.join(" ")}\nexit code: ${code}\n\n${maskSensitive(tail(output))}`);
}

const server = new McpServer({ name: "android-dev-mcp", version: "0.1.0" });

server.registerTool("gradle_task", {
  title: "Run an allow-listed Gradle task",
  description: `Runs one of: ${Object.keys(GRADLE_TASKS).join(", ")} for the whole project or a module (e.g. ":app"). Returns exit code and the last 200 lines of output.`,
  inputSchema: {
    task: z.enum(Object.keys(GRADLE_TASKS) as [GradleTask, ...GradleTask[]]),
    module: z.string().optional().describe("Gradle module path, e.g. :app or :feature:claims"),
  },
}, async ({ task, module }) => gradle(task, module));

server.registerTool("gradle_test", {
  title: "Run unit tests",
  description: "Shortcut for gradle_task test. Use after every code change.",
  inputSchema: { module: z.string().optional() },
}, async ({ module }) => gradle("test", module));

server.registerTool("gradle_lint", {
  title: "Run Android lint",
  description: "Shortcut for gradle_task lint.",
  inputSchema: { module: z.string().optional() },
}, async ({ module }) => gradle("lint", module));

server.registerTool("adb_devices", {
  title: "List connected devices and emulators",
  description: "Runs `adb devices -l`.",
  inputSchema: {},
}, async () => {
  const { code, output } = await run(ADB, ["devices", "-l"], 15_000);
  return text(code === 0 ? output.trim() : `adb failed (${code}): ${output}`);
});

server.registerTool("logcat_tail", {
  title: "Read recent logcat lines",
  description: "Returns up to 500 recent logcat lines, optionally filtered by tag and minimum priority. Personal data (PESEL, IBAN, e-mail, phone, policy numbers, bearer tokens) is masked before it reaches the model.",
  inputSchema: {
    lines: z.number().int().min(1).max(500).default(200),
    tag: z.string().optional(),
    priority: z.enum(PRIORITIES).default("I"),
    serial: z.string().optional().describe("Device serial from adb_devices"),
  },
}, async ({ lines, tag, priority, serial }) => {
  let args: string[];
  try {
    args = logcatArgs(lines, tag, priority, serial);
  } catch (e) {
    return { ...text(`Rejected: ${(e as Error).message}`), isError: true };
  }
  const { code, output } = await run(ADB, args, 20_000);
  return text(code === 0 ? maskSensitive(output.trim()) : `adb logcat failed (${code}): ${output}`);
});

server.registerTool("run_ui_tests", {
  title: "Run instrumented UI tests",
  description: "Runs connectedDebugAndroidTest (Espresso / Compose Test Rule) on the connected device for a module.",
  inputSchema: { module: z.string().default(":app") },
}, async ({ module }) => gradle("connectedAndroidTest", module));

await server.connect(new StdioServerTransport());
