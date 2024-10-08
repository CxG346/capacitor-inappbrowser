package ee.forgr.capacitor_inappbrowser;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.PermissionRequest;
import androidx.browser.customtabs.CustomTabsCallback;
import androidx.browser.customtabs.CustomTabsClient;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.browser.customtabs.CustomTabsServiceConnection;
import androidx.browser.customtabs.CustomTabsSession;
import com.getcapacitor.JSObject;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.ActivityCallback;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;

import java.lang.StackWalker.Option;
import java.util.Iterator;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Color;
import android.widget.RelativeLayout;
import android.widget.LinearLayout;
import android.widget.Button;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.view.ViewGroup;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;
import android.widget.Toast;
import android.content.Context;


@CapacitorPlugin(name = "InAppBrowser", permissions = {
    @Permission(alias = "camera", strings = { Manifest.permission.CAMERA }),
    @Permission(alias = "storage", strings = { Manifest.permission.READ_EXTERNAL_STORAGE }),
    @Permission(alias = "storage", strings = { Manifest.permission.READ_MEDIA_IMAGES }),
}, requestCodes = { WebViewDialog.FILE_CHOOSER_REQUEST_CODE })
public class InAppBrowserPlugin
    extends Plugin
    implements WebViewDialog.PermissionHandler {

  public static final String CUSTOM_TAB_PACKAGE_NAME = "com.android.chrome"; // Change when in stable
  private CustomTabsClient customTabsClient;
  private CustomTabsSession currentSession;
  private WebViewDialog webViewDialog = null;
  private String currentUrl = "";

  private PermissionRequest currentPermissionRequest;
  private WebView webView;
  private Context context;

  public boolean hasListeners(String eventName) {
    return super.hasListeners(eventName);
  }

  public void notifyListeners(String eventName, JSObject data) {
    super.notifyListeners(eventName, data);
  }

  public void handleCameraPermissionRequest(PermissionRequest request) {
    this.currentPermissionRequest = request;
    if (getPermissionState("camera") != PermissionState.GRANTED) {
      requestPermissionForAlias("camera", null, "cameraPermissionCallback");
    } else {
      grantCameraPermission();
    }
  }

  @Override
  protected void handleOnActivityResult(
      int requestCode,
      int resultCode,
      Intent data) {
    super.handleOnActivityResult(requestCode, resultCode, data);

    // Check if the request code matches the file chooser request code
    if (requestCode == WebViewDialog.FILE_CHOOSER_REQUEST_CODE) {
      if (webViewDialog != null && webViewDialog.mFilePathCallback != null) {
        Uri[] results = null;

        if (resultCode == Activity.RESULT_OK) {
          if (data != null) {
            results = new Uri[] { data.getData() };
          }
        }

        // Pass the results back to the WebView
        try {
          webViewDialog.mFilePathCallback.onReceiveValue(results);
          webViewDialog.mFilePathCallback = null;
        } catch (Exception e) {
          Log.e("ACTIVITYRESULT", e.getMessage());
        }
      }
    }
  }

  @PermissionCallback
  private void cameraPermissionCallback() {
    if (getPermissionState("camera") == PermissionState.GRANTED) {
      grantCameraPermission();
    } else {
      if (currentPermissionRequest != null) {
        currentPermissionRequest.deny();
        currentPermissionRequest = null;
      }
      // Handle the case where permission was not granted
    }
  }

  private void grantCameraPermission() {
    if (currentPermissionRequest != null) {
      currentPermissionRequest.grant(
          new String[] { PermissionRequest.RESOURCE_VIDEO_CAPTURE });
      currentPermissionRequest = null;
    }
  }

  CustomTabsServiceConnection connection = new CustomTabsServiceConnection() {
    @Override
    public void onCustomTabsServiceConnected(
        ComponentName name,
        CustomTabsClient client) {
      customTabsClient = client;
      client.warmup(0);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
    }
  };

  @Override
  public void load() {
    super.load();
    webView = new WebView(getContext());
    webView.getSettings().setJavaScriptEnabled(true);

    // Configurar webView según sea necesario
  }

  @PluginMethod
  public void shareFunction() {
  }

  @PluginMethod
  public void openWeb(PluginCall call) {
    Log.d("WebViewDialog", "All data received: " + call.getData().toString());
    String url = call.getString("url");

    if (url == null || TextUtils.isEmpty(url)) {
      call.reject("Invalid URL");
    }
    currentUrl = url;
    final Options options = new Options();
    options.setUrl(url);
    options.setHeaders(call.getObject("headers"));
    options.setShowReloadButton(call.getBoolean("showReloadButton", false));
    options.setVisibleTitle(call.getBoolean("visibleTitle", true));
    options.setCustomTextShareButton(call.getString("customTextShareButton", "Share"));
    options.setColorShareButton(call.getString("colorShareButton", "#00BFFF"));
    options.setColorShareText(call.getString("colorShareText", "#ffffff"));
    options.setShareFunction(call.getBoolean("shareFunction", false));
    options.setPrintFunction(call.getBoolean("printFunction", false));
    if (Boolean.TRUE.equals(options.getVisibleTitle())) {
      options.setTitle(call.getString("title", "New Window"));
      options.setColorTitle(call.getString("colorTitle", "#000000"));
    } else {
      options.setTitle(call.getString("title", ""));
    }
    options.setToolbarColor(call.getString("toolbarColor", "#ffffff"));
    options.setArrow(Boolean.TRUE.equals(call.getBoolean("showArrow", false)));
    options.setShareButton(Boolean.TRUE.equals(call.getBoolean("showShareButton", false)));
    options.setDownloadButton(Boolean.TRUE.equals(call.getBoolean("showDownloadButton", false)));
    options.setNavigationButtons(Boolean.TRUE.equals(call.getBoolean("showNavigationButtons", false)));
    options.setIgnoreUntrustedSSLError(Boolean.TRUE.equals(call.getBoolean("ignoreUntrustedSSLError", false)));
    options.setShareDisclaimer(call.getObject("shareDisclaimer", null));
    options.setShareSubject(call.getString("shareSubject", null));
    options.setToolbarType(call.getString("toolbarType", ""));
    options.setActiveNativeNavigationForWebview(call.getBoolean("activeNativeNavigationForWebview", false));
    options.setDisableGoBackOnNativeApplication(call.getBoolean("disableGoBackOnNativeApplication", false));
    options.setPresentAfterPageLoad(call.getBoolean("isPresentAfterPageLoad", false));
    if (call.getBoolean("closeModal", false)) {
      options.setCloseModal(true);
      options.setCloseModalTitle(call.getString("closeModalTitle", "Close"));
      options.setCloseModalDescription(call.getString("closeModalDescription", "Are you sure ?"));
      options.setCloseModalOk(call.getString("closeModalOk", "Ok"));
      options.setCloseModalCancel(call.getString("closeModalCancel", "Cancel"));
    } else {
      options.setCloseModal(false);
    }

    String browserPosition = call.getString("browserPosition", "top");
    options.setBrowserPosition(browserPosition);

    options.setPluginCall(call);
    options.setCallbacks(
        new WebViewCallbacks() {
          @Override
          public void urlChangeEvent(String url) {
            notifyListeners("urlChangeEvent", new JSObject().put("url", url));
          }

          @Override
          public void closeEvent(String url) {
            notifyListeners("closeEvent", new JSObject().put("url", url));
          }

          @Override
          public void pageLoaded() {
            notifyListeners("browserPageLoaded", new JSObject());
          }

          @Override
          public void pageLoadError() {
            notifyListeners("pageLoadError", new JSObject());
          }

          @Override
          public void shareButtonClicked() {
            Log.d("InAppBrowser", "Share button clicked");
            notifyListeners("shareButtonClicked", new JSObject());
          }

          @Override
          public void downloadButtonClicked() {
            notifyListeners("downloadButtonClicked", new JSObject());
          }
        });

    this.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        try {
          webViewDialog = new WebViewDialog(
              getContext(),
              android.R.style.Theme_NoTitleBar,
              options,
              InAppBrowserPlugin.this);

          webViewDialog.setWebViewCallbacks(new WebViewCallbacks() {
            @Override
            public void urlChangeEvent(String url) {
              notifyListeners("urlChangeEvent", new JSObject().put("url", url));
            }

            @Override
            public void closeEvent(String url) {
              notifyListeners("closeEvent", new JSObject().put("url", url));
            }

            @Override
            public void pageLoaded() {
              notifyListeners("browserPageLoaded", new JSObject());
            }

            @Override
            public void pageLoadError() {
              notifyListeners("pageLoadError", new JSObject());
            }

            @Override
            public void shareButtonClicked() {
              notifyListeners("shareButtonClicked", new JSObject());
            }

            @Override
            public void downloadButtonClicked() {
              notifyListeners("downloadButtonClicked", new JSObject());
            }
          });

          webViewDialog.setPosition(browserPosition);
          webViewDialog.presentWebView();
          webViewDialog.activity = InAppBrowserPlugin.this.getActivity();

        } catch (Exception e) {
          call.reject("Error initializing WebViewDialog: " + e.getMessage());
        }
      }
    });
  }

  @PluginMethod
  public void requestCameraPermission(PluginCall call) {
    if (getPermissionState("camera") != PermissionState.GRANTED) {
      requestPermissionForAlias("camera", call, "cameraPermissionCallback");
    } else {
      call.resolve();
    }
  }

  @PermissionCallback
  private void cameraPermissionCallback(PluginCall call) {
    if (getPermissionState("camera") == PermissionState.GRANTED) {
      // Permission granted, notify the WebView to proceed
      if (webViewDialog != null && webViewDialog.currentPermissionRequest != null) {
        webViewDialog.currentPermissionRequest.grant(
            new String[] { PermissionRequest.RESOURCE_VIDEO_CAPTURE });
        webViewDialog.currentPermissionRequest = null;
      }
      call.resolve();
    } else {
      call.reject("Camera permission is required");
    }
  }

  @PluginMethod
  public void setUrl(PluginCall call) {
    String url = call.getString("url");
    if (url == null || TextUtils.isEmpty(url)) {
      call.reject("Invalid URL");
    }
    currentUrl = url;
    this.getActivity()
        .runOnUiThread(
            new Runnable() {
              @Override
              public void run() {
                webViewDialog.setUrl(url);
              }
            });
    call.resolve();
  }

  @PluginMethod
  public void open(PluginCall call) {
    String url = call.getString("url");

    // get the deeplink prevention, if provided
    Boolean preventDeeplink = call.getBoolean("preventDeeplink", null);

    if (url == null || TextUtils.isEmpty(url)) {
      call.reject("Invalid URL");
    }
    currentUrl = url;
    CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder(
        getCustomTabsSession());
    CustomTabsIntent tabsIntent = builder.build();
    tabsIntent.intent.putExtra(
        Intent.EXTRA_REFERRER,
        Uri.parse(
            Intent.URI_ANDROID_APP_SCHEME + "//" + getContext().getPackageName()));
    tabsIntent.intent.putExtra(
        android.provider.Browser.EXTRA_HEADERS,
        this.getHeaders(call));

    if (preventDeeplink != null) {
      String browserPackageName = "";
      Intent browserIntent = new Intent(
          Intent.ACTION_VIEW,
          Uri.parse("http://"));
      ResolveInfo resolveInfo = getContext()
          .getPackageManager()
          .resolveActivity(browserIntent, PackageManager.MATCH_DEFAULT_ONLY);

      if (resolveInfo != null) {
        browserPackageName = resolveInfo.activityInfo.packageName;

        if (!browserPackageName.isEmpty()) {
          tabsIntent.intent.setPackage(browserPackageName);
        }
      }
    }

    tabsIntent.launchUrl(getContext(), Uri.parse(url));

    call.resolve();
  }

  @PluginMethod
  public void clearCookies(PluginCall call) {
    String url = call.getString("url");
    Boolean clearCache = call.getBoolean("cache", false);
    if (url == null || TextUtils.isEmpty(url)) {
      call.reject("Invalid URL");
    } else {
      CookieManager cookieManager = CookieManager.getInstance();
      String cookie = cookieManager.getCookie(url);
      if (cookie != null) {
        cookieManager.removeAllCookies(null);
        if (Boolean.TRUE.equals(clearCache)) {
          cookieManager.removeSessionCookies(null);
        }
      }
      call.resolve();
    }
  }

  @PluginMethod
  public void getCookies(PluginCall call) {
    String url = call.getString("url");
    if (url == null || TextUtils.isEmpty(url)) {
      call.reject("Invalid URL");
    } else {
      CookieManager cookieManager = CookieManager.getInstance();
      String cookieString = cookieManager.getCookie(url);
      if (cookieString != null) {
        String[] cookiePairs = cookieString.split("; ");
        JSObject result = new JSObject();
        for (String cookie : cookiePairs) {
          String[] parts = cookie.split("=", 2);
          if (parts.length == 2) {
            result.put(parts[0], parts[1]);
          }
        }
        call.resolve(result);
      }
      call.resolve(new JSObject());
    }
  }

  @PluginMethod
  public void openWebView(PluginCall call) {
    String url = call.getString("url");
    if (url == null || TextUtils.isEmpty(url)) {
      call.reject("Invalid URL");
    }
    currentUrl = url;
    final Options options = new Options();
    options.setUrl(url);
    options.setHeaders(call.getObject("headers"));
    options.setShowReloadButton(call.getBoolean("showReloadButton", false));
    options.setVisibleTitle(call.getBoolean("visibleTitle", true));
    if (Boolean.TRUE.equals(options.getVisibleTitle())) {
      options.setTitle(call.getString("title", "New Window"));
    } else {
      options.setTitle(call.getString("title", ""));
    }
    options.setToolbarColor(call.getString("toolbarColor", "#ffffff"));
    options.setArrow(Boolean.TRUE.equals(call.getBoolean("showArrow", false)));
    options.setIgnoreUntrustedSSLError(
        Boolean.TRUE.equals(call.getBoolean("ignoreUntrustedSSLError", false)));
    options.setShareDisclaimer(call.getObject("shareDisclaimer", null));
    options.setShareSubject(call.getString("shareSubject", null));
    options.setToolbarType(call.getString("toolbarType", ""));
    options.setActiveNativeNavigationForWebview(
        call.getBoolean("activeNativeNavigationForWebview", false));
    options.setDisableGoBackOnNativeApplication(
        call.getBoolean("disableGoBackOnNativeApplication", false));
    options.setPresentAfterPageLoad(
        call.getBoolean("isPresentAfterPageLoad", false));
    if (call.getBoolean("closeModal", false)) {
      options.setCloseModal(true);
      options.setCloseModalTitle(call.getString("closeModalTitle", "Close"));
      options.setCloseModalDescription(
          call.getString("closeModalDescription", "Are you sure ?"));
      options.setCloseModalOk(call.getString("closeModalOk", "Ok"));
      options.setCloseModalCancel(call.getString("closeModalCancel", "Cancel"));
    } else {
      options.setCloseModal(false);
    }
    options.setPluginCall(call);
    // options.getToolbarItemTypes().add(ToolbarItemType.RELOAD); TODO: fix this
    options.setCallbacks(
        new WebViewCallbacks() {
          @Override
          public void urlChangeEvent(String url) {
            notifyListeners("urlChangeEvent", new JSObject().put("url", url));
          }

          @Override
          public void closeEvent(String url) {
            notifyListeners("closeEvent", new JSObject().put("url", url));
          }

          @Override
          public void pageLoaded() {
            notifyListeners("browserPageLoaded", new JSObject());
          }

          @Override
          public void pageLoadError() {
            notifyListeners("pageLoadError", new JSObject());
          }

          @Override
          public void shareButtonClicked() {
            notifyListeners("shareButtonClicked", new JSObject());
          }

          @Override
          public void downloadButtonClicked() {
            notifyListeners("downloadButtonClicked", new JSObject());
          }
        });
    this.getActivity()
        .runOnUiThread(
            new Runnable() {
              @Override
              public void run() {
                webViewDialog = new WebViewDialog(
                    getContext(),
                    android.R.style.Theme_NoTitleBar,
                    options,
                    InAppBrowserPlugin.this);
                webViewDialog.presentWebView();
                webViewDialog.activity = InAppBrowserPlugin.this.getActivity();
              }
            });
  }

  @PluginMethod
  public void executeScript(PluginCall call) {
    String script = call.getString("code");
    if (script == null || TextUtils.isEmpty(script)) {
      call.reject("No script to run");
    }

    if (webViewDialog != null) {
      this.getActivity()
          .runOnUiThread(
              new Runnable() {
                @Override
                public void run() {
                  webViewDialog.executeScript(script);
                }
              });
    }

    call.resolve();
  }

  @PluginMethod
  public void reload(PluginCall call) {
    if (webViewDialog != null) {
      this.getActivity()
          .runOnUiThread(
              new Runnable() {
                @Override
                public void run() {
                  webViewDialog.reload();
                }
              });
    }
    call.resolve();
  }

  @PluginMethod
  public void close(PluginCall call) {
    this.getActivity()
        .runOnUiThread(
            new Runnable() {
              @Override
              public void run() {
                if (webViewDialog != null) {
                  notifyListeners(
                      "closeEvent",
                      new JSObject().put("url", webViewDialog.getUrl()));
                  webViewDialog.dismiss();
                  webViewDialog.destroy();
                  webViewDialog = null;
                } else {
                  Intent intent = new Intent(
                      getContext(),
                      getBridge().getActivity().getClass());
                  intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                  getContext().startActivity(intent);
                }
                call.resolve();
              }
            });
  }

  private Bundle getHeaders(PluginCall pluginCall) {
    JSObject headersProvided = pluginCall.getObject("headers");
    Bundle headers = new Bundle();
    if (headersProvided != null) {
      Iterator<String> keys = headersProvided.keys();
      while (keys.hasNext()) {
        String key = keys.next();
        headers.putString(key, headersProvided.getString(key));
      }
    }
    return headers;
  }

  protected void handleOnResume() {
    boolean ok = CustomTabsClient.bindCustomTabsService(
        getContext(),
        CUSTOM_TAB_PACKAGE_NAME,
        connection);
    if (!ok) {
      Log.e(getLogTag(), "Error binding to custom tabs service");
    }
  }

  protected void handleOnPause() {
    getContext().unbindService(connection);
  }

  public CustomTabsSession getCustomTabsSession() {
    if (customTabsClient == null) {
      return null;
    }

    if (currentSession == null) {
      currentSession = customTabsClient.newSession(
          new CustomTabsCallback() {
            @Override
            public void onNavigationEvent(int navigationEvent, Bundle extras) {
              switch (navigationEvent) {
                case NAVIGATION_FINISHED:
                  notifyListeners("browserPageLoaded", new JSObject());
                  break;
              }
            }
          });
    }
    return currentSession;
  }

}
