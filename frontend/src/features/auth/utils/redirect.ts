import { HOME_PATH } from '@/lib/paths';

/** Only paths inside this app ("/cart"), never another site: no open redirect after login. */
export function safeRedirect(target: string | null): string {
  return target && target.startsWith('/') && !target.startsWith('//') ? target : HOME_PATH;
}
