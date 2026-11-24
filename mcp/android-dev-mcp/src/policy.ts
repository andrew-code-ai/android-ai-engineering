/**
 * Polityka bezpieczeństwa serwera: agent NIE dostaje dowolnego shella.
 * Wszystkie komendy są budowane tutaj z walidowanych parametrów i uruchamiane
 * przez execFile (bez interpretera powłoki), z limitem czasu i rozmiaru wyjścia.
 */

export const GRADLE_TASKS = {
  lint: "lint",
  test: "test",
  assembleDebug: "assembleDebug",
  detekt: "detekt",
  ktlintCheck: "ktlintCheck",
  connectedAndroidTest: "connectedDebugAndroidTest",
} as const;
export type GradleTask = keyof typeof GRADLE_TASKS;

const MODULE_RE = /^:[A-Za-z0-9_-]+(:[A-Za-z0-9_-]+)*$/;
const TAG_RE = /^[A-Za-z0-9_.-]{1,64}$/;
const SERIAL_RE = /^[A-Za-z0-9._:-]{1,64}$/;
export const PRIORITIES = ["V", "D", "I", "W", "E"] as const;
export type Priority = (typeof PRIORITIES)[number];

export function gradleArgs(task: GradleTask, module?: string): string[] {
  if (!(task in GRADLE_TASKS)) throw new Error(`Task not allowed: ${task}`);
  if (module !== undefined && !MODULE_RE.test(module)) throw new Error(`Invalid module path: ${module}`);
  const name = GRADLE_TASKS[task];
  return [module ? `${module}:${name}` : name, "--console=plain", "--no-daemon"];
}

export function logcatArgs(lines: number, tag?: string, priority: Priority = "I", serial?: string): string[] {
  if (!Number.isInteger(lines) || lines < 1 || lines > 500) throw new Error("lines must be 1..500");
  if (tag !== undefined && !TAG_RE.test(tag)) throw new Error(`Invalid tag: ${tag}`);
  if (!PRIORITIES.includes(priority)) throw new Error(`Invalid priority: ${priority}`);
  if (serial !== undefined && !SERIAL_RE.test(serial)) throw new Error(`Invalid serial: ${serial}`);
  const base = serial ? ["-s", serial] : [];
  const filter = tag ? [`${tag}:${priority}`, "*:S"] : [`*:${priority}`];
  return [...base, "logcat", "-d", "-t", String(lines), ...filter];
}

/** Zostawia ostatnie `maxLines` linii — błędy Gradle są na końcu wyjścia. */
export function tail(text: string, maxLines = 200): string {
  const lines = text.split("\n");
  return lines.length <= maxLines ? text : `…(${lines.length - maxLines} lines omitted)\n` + lines.slice(-maxLines).join("\n");
}

// ---- maskowanie danych wrażliwych w logcat (logi aplikacji bywają brudne)

function peselValid(d: string): boolean {
  const w = [1, 3, 7, 9, 1, 3, 7, 9, 1, 3];
  const sum = w.reduce((acc, wi, i) => acc + wi * Number(d[i]), 0);
  const month = Number(d.slice(2, 4)) % 20;
  return (10 - (sum % 10)) % 10 === Number(d[10]) && month >= 1 && month <= 12;
}

export function maskSensitive(text: string): string {
  return text
    .replace(/\b[\w.+-]+@[\w-]+(?:\.[\w-]+)+\b/g, "[EMAIL]")
    .replace(/\bPL\s?\d{2}(?:\s?\d{4}){6}\b/gi, "[IBAN]")
    .replace(/(?<!\d)\d{11}(?!\d)/g, (m) => (peselValid(m) ? "[PESEL]" : m))
    .replace(/\bPOL-\d{4}-\d{8}\b/g, "[POLICY_NO]")
    .replace(/(?<![\w+])(?:\+48[ -]?)?\d{3}[ -]\d{3}[ -]\d{3}(?!\d)/g, "[PHONE]")
    .replace(/(Bearer\s+)[A-Za-z0-9._-]{16,}/g, "$1[TOKEN]");
}
