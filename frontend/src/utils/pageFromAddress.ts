/** Anything but a whole number from 1 upwards means page 1. */
export function pageFromAddress(value: string | null): number {
  const page = Number(value);
  return Number.isInteger(page) && page >= 1 ? page : 1;
}
