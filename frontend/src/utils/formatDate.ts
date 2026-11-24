const dateFormatter = new Intl.DateTimeFormat('en-IE', { dateStyle: 'medium' });

/** "2026-11-22T12:00:00Z" -> "22 Nov 2026", in the user's own time zone (same locale as formatPrice). */
export function formatDate(isoTimestamp: string): string {
  return dateFormatter.format(new Date(isoTimestamp));
}
