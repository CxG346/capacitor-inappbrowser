import { WebPlugin } from "@capacitor/core";

import type {
  InAppBrowserPlugin,
  OpenWebViewOptions,
  OpenOptions,
  GetCookieOptions,
  ClearCookieOptions,
  NewOpenOptions,
  NewOpenWebOptions,
  shareFunctionOptions,
} from "./definitions";

export class InAppBrowserWeb extends WebPlugin implements InAppBrowserPlugin {
  
  private browserWindow: Window | null = null;

  async open(options: OpenOptions): Promise<any> {
    console.log("open", options);
    return options;
  }

  async newOpen(options: NewOpenOptions): Promise<any> {
    const { url, headerBackgroundColor, closeButton, navigationButtons, title, downloadButton, refreshButton, shareButton } = options;

    // Open a new window with the provided URL
    this.browserWindow = window.open(url, "_blank");

    if (!this.browserWindow) {
      throw new Error("Failed to open the browser window");
    }

    // Apply the provided options
    if (headerBackgroundColor) {
      this.browserWindow.document.body.style.backgroundColor = headerBackgroundColor;
    }

    // Add event listeners for buttons
    if (closeButton?.display) {
      const closeButtonElement = this.createButton("Close", closeButton.color || "defaultColor");
      closeButtonElement.addEventListener("click", () => this.close());
      this.browserWindow.document.body.appendChild(closeButtonElement);
    }
    
    if (navigationButtons?.display) {
      const backButton = this.createButton("Back", navigationButtons.color || "defaultColor");
      backButton.addEventListener("click", () => this.browserWindow?.history.back());
      const forwardButton = this.createButton("Forward", navigationButtons.color || "defaultColor");
      forwardButton.addEventListener("click", () => this.browserWindow?.history.forward());
      this.browserWindow.document.body.appendChild(backButton);
      this.browserWindow.document.body.appendChild(forwardButton);
    }
    
    if (title?.display) {
      const titleElement = document.createElement("h1");
      titleElement.innerText = title.label || "";
      titleElement.style.color = title.color || "defaultColor";
      this.browserWindow.document.body.appendChild(titleElement);
    }
    
    if (downloadButton?.display) {
      const downloadButtonElement = this.createButton("Download", downloadButton.color || "defaultColor");
      downloadButtonElement.addEventListener("click", () => this.downloadContent());
      this.browserWindow.document.body.appendChild(downloadButtonElement);
    }
    
    if (refreshButton?.display) {
      const refreshButtonElement = this.createButton("Refresh", refreshButton.color || "defaultColor");
      refreshButtonElement.addEventListener("click", () => this.browserWindow?.location.reload());
      this.browserWindow.document.body.appendChild(refreshButtonElement);
    }

    if (shareButton?.display) {
      const shareButtonElement = this.createButton(shareButton.label || "Share", shareButton.textColor || "defaultColor");
      shareButtonElement.style.backgroundColor = shareButton.backgroundColor || "";
      shareButtonElement.addEventListener("click", () => this.shareContent());
      this.browserWindow.document.body.appendChild(shareButtonElement);
    }

    return options;
  }

  createButton(label: string, color: string): HTMLButtonElement {
    const button = document.createElement("button");
    button.innerText = label;
    button.style.backgroundColor = color;
    return button;
  }

  downloadContent(): void {
    console.log("Download content");
    this.notifyListeners('onDownloadButtonClicked', {});
  }

  shareContent(): void {
    console.log("Share content");
    this.notifyListeners('onShareButtonClicked', {});
  }

  async clearCookies(options: ClearCookieOptions): Promise<any> {
    console.log("clearCookies", options);
    // Implement logic to clear cookies
    return;
  }

  async getCookies(options: GetCookieOptions): Promise<any> {
    console.log("getCookies", options);
    // Implement logic to get cookies
    return options;
  }

  async openWebView(options: OpenWebViewOptions): Promise<any> {
    console.log("openWebView", options);
    // Implement logic to open web view
    return options;
  }

  async executeScript({ code }: { code: string }): Promise<any> {
    console.log("executeScript", code);
    // Implement logic to execute script
    return code;
  }

  async close(): Promise<any> {
    if (this.browserWindow) {
      this.browserWindow.close();
      this.browserWindow = null;
      console.log("Browser window closed");
    }
    return;
  }

  async setUrl(options: { url: string }): Promise<any> {
    if (this.browserWindow) {
      this.browserWindow.location.href = options.url;
      console.log("URL set to", options.url);
    }
    return;
  }

  async reload(): Promise<any> {
    if (this.browserWindow) {
      this.browserWindow.location.reload();
      console.log("Browser window reloaded");
    }
    return;
  }

  async openWeb(options: NewOpenWebOptions): Promise<any> {
    console.log("openWeb", options);
    // Implement logic to open web view
    return options;
  }

  async shareFunction(options: shareFunctionOptions): Promise<any> {
    // Implementación de shareFunction
    console.log('shareFunction called');
    alert('shareFunction called');

    return options;
  }
}