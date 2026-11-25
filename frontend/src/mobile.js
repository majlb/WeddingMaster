import { Capacitor } from "@capacitor/core";
import { App as CapacitorApp } from "@capacitor/app";

export function initializeMobile() {
  if (!Capacitor.isNativePlatform()) return;

  CapacitorApp.addListener("backButton", ({ canGoBack }) => {
    if (canGoBack) {
      window.history.back();
    } else {
      CapacitorApp.exitApp().catch((error) => console.error("Could not exit the app", error));
    }
  }).catch((error) => console.error("Could not register Android back button", error));

  document.addEventListener("click", (event) => {
    if (!(event.target instanceof Element)) return;
    const link = event.target.closest("a[target='_blank']");
    if (!link) return;
    if ((link.protocol === "https:" || link.protocol === "http:") && link.origin !== window.location.origin) {
      link.target = "_self";
    }
  }, true);
}
