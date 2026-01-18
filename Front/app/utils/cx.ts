export function cx(...classes: (string | undefined | null | false)[]): string {
  return classes.filter(Boolean).join(' ');
}

export function sortCx(obj: Record<string, string>): Record<string, string> {
  const sorted: Record<string, string> = {};
  Object.keys(obj).sort().forEach(key => {
    sorted[key] = obj[key];
  });
  return sorted;
}
