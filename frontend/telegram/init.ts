import WebApp from "@twa-dev/sdk";

let initialized = false;

export function initTelegramWebApp(): void {
  if (initialized) {
    return;
  }

  WebApp.ready();
  WebApp.expand();
  initialized = true;
}

export { WebApp };
