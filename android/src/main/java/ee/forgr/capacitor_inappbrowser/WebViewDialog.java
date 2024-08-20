package ee.forgr.capacitor_inappbrowser;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.net.http.SslError;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.PermissionRequest;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import android.print.PrintManager;
import android.print.PrintDocumentAdapter;
import android.print.PrintAttributes;
import android.graphics.Bitmap;
import com.getcapacitor.JSObject;
import android.graphics.BitmapFactory;

public class WebViewDialog extends Dialog {

  private WebView _webView;
  private Toolbar _toolbar;
  private Options _options;
  private Context _context;
  public Activity activity;
  private boolean isInitialized = false;
  private String position;

  public PermissionRequest currentPermissionRequest;
  public static final int FILE_CHOOSER_REQUEST_CODE = 1000;
  public ValueCallback<Uri> mUploadMessage;
  public ValueCallback<Uri[]> mFilePathCallback;
  private WebViewCallbacks webViewCallbacks;
  private InAppBrowserPlugin _plugin;

  public WebViewDialog(Context context, int themeResId, Options options) {
    super(context, themeResId);
    // Configuración del WebViewDialog
  }

  public void setWebViewCallbacks(WebViewCallbacks callbacks) {
    this.webViewCallbacks = callbacks;
  }

  private void notifyShareButtonClicked() {
    if (webViewCallbacks != null) {
      webViewCallbacks.shareButtonClicked();
    }else{
      Log.d("WebViewDialog", "webViewCallbacks is null");
    }
  }

  private void notifyDownloadButtonClicked() {
    if (webViewCallbacks != null) {
      webViewCallbacks.downloadButtonClicked();
    }else{
      Log.d("WebViewDialog", "webViewCallbacks is null");
    }
  }

  public interface PermissionHandler {
    void handleCameraPermissionRequest(PermissionRequest request);
  }

  private PermissionHandler permissionHandler;

  public WebViewDialog(
      Context context,
      int theme,
      Options options,
      PermissionHandler permissionHandler) {
    super(context, theme);
    this._options = options;
    this._context = context;
    this.permissionHandler = permissionHandler;
    this.isInitialized = false;
  }

  public void presentWebView() {
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    setCancelable(true);
    getWindow()
        .setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN);
    setContentView(R.layout.activity_browser);
    getWindow()
        .setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT);

    this._webView = findViewById(R.id.browser_view);

    _webView.getSettings().setJavaScriptEnabled(true);
    _webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
    _webView.getSettings().setDatabaseEnabled(true);
    _webView.getSettings().setDomStorageEnabled(true);
    _webView.getSettings().setAllowFileAccess(true);
    _webView
        .getSettings()
        .setPluginState(android.webkit.WebSettings.PluginState.ON);
    _webView.getSettings().setLoadWithOverviewMode(true);
    _webView.getSettings().setUseWideViewPort(true);
    _webView.getSettings().setAllowFileAccessFromFileURLs(true);
    _webView.getSettings().setAllowUniversalAccessFromFileURLs(true);

    _webView.setWebViewClient(new WebViewClient());

    _webView.setWebChromeClient(
        new WebChromeClient() {
          // Enable file open dialog
          @Override
          public boolean onShowFileChooser(
              WebView webView,
              ValueCallback<Uri[]> filePathCallback,
              WebChromeClient.FileChooserParams fileChooserParams) {
            openFileChooser(
                filePathCallback,
                fileChooserParams.getAcceptTypes()[0]);
            return true;
          }

          // Grant permissions for cam
          @Override
          public void onPermissionRequest(final PermissionRequest request) {
            Log.i(
                "INAPPBROWSER",
                "onPermissionRequest " + request.getResources().toString());
            final String[] requestedResources = request.getResources();
            for (String r : requestedResources) {
              Log.i("INAPPBROWSER", "requestedResources " + r);
              if (r.equals(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) {
                Log.i("INAPPBROWSER", "RESOURCE_VIDEO_CAPTURE req");
                // Store the permission request
                currentPermissionRequest = request;
                // Initiate the permission request through the plugin
                if (permissionHandler != null) {
                  permissionHandler.handleCameraPermissionRequest(request);
                }
                break;
              }
            }
          }

          @Override
          public void onPermissionRequestCanceled(PermissionRequest request) {
            super.onPermissionRequestCanceled(request);
            Toast.makeText(
                WebViewDialog.this.activity,
                "Permission Denied",
                Toast.LENGTH_SHORT).show();
            // Handle the denied permission
            if (currentPermissionRequest != null) {
              currentPermissionRequest.deny();
              currentPermissionRequest = null;
            }
          }
        });

    Map<String, String> requestHeaders = new HashMap<>();
    if (_options.getHeaders() != null) {
      Iterator<String> keys = _options.getHeaders().keys();
      while (keys.hasNext()) {
        String key = keys.next();
        if (TextUtils.equals(key.toLowerCase(), "user-agent")) {
          _webView
              .getSettings()
              .setUserAgentString(_options.getHeaders().getString(key));
        } else {
          requestHeaders.put(key, _options.getHeaders().getString(key));
        }
      }
    }

    _webView.loadUrl(this._options.getUrl(), requestHeaders);
    _webView.requestFocus();
    _webView.requestFocusFromTouch();

    setupToolbar();
    setWebViewClient();

    if (!this._options.isPresentAfterPageLoad()) {
      show();
      _options.getPluginCall().resolve();
    }
  }

  private void openFileChooser(
      ValueCallback<Uri[]> filePathCallback,
      String acceptType) {
    mFilePathCallback = filePathCallback;
    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
    intent.addCategory(Intent.CATEGORY_OPENABLE);
    intent.setType(acceptType); // Default to */*
    activity.startActivityForResult(
        Intent.createChooser(intent, "Select File"),
        FILE_CHOOSER_REQUEST_CODE);
  }

  public void reload() {
    _webView.reload();
  }

  public void destroy() {
    _webView.destroy();
  }

  public String getUrl() {
    return _webView.getUrl();
  }

  public void executeScript(String script) {
    _webView.evaluateJavascript(script, null);
  }

  public void setUrl(String url) {
    Map<String, String> requestHeaders = new HashMap<>();
    if (_options.getHeaders() != null) {
      Iterator<String> keys = _options.getHeaders().keys();
      while (keys.hasNext()) {
        String key = keys.next();
        if (TextUtils.equals(key.toLowerCase(), "user-agent")) {
          _webView
              .getSettings()
              .setUserAgentString(_options.getHeaders().getString(key));
        } else {
          requestHeaders.put(key, _options.getHeaders().getString(key));
        }
      }
    }
    _webView.loadUrl(url, requestHeaders);
  }

  private void setTitle(String newTitleText) {
    TextView textView = (TextView) _toolbar.findViewById(R.id.titleText);
    if (_options.getVisibleTitle()) {
      textView.setText(newTitleText);
    } else {
      textView.setText("");
    }
  }

  private void setupToolbar() {
    _toolbar = this.findViewById(R.id.tool_bar);
    int color = Color.parseColor("#ffffff");
    try {
      color = Color.parseColor(_options.getToolbarColor());
    } catch (IllegalArgumentException e) {
      // Do nothing
    }
    _toolbar.setBackgroundColor(color);
    _toolbar.findViewById(R.id.backButton).setBackgroundColor(color);
    _toolbar.findViewById(R.id.forwardButton).setBackgroundColor(color);
    _toolbar.findViewById(R.id.closeButton).setBackgroundColor(color);
    _toolbar.findViewById(R.id.reloadButton).setBackgroundColor(color);
    _toolbar.findViewById(R.id.shareButton).setBackgroundColor(color);
    _toolbar.findViewById(R.id.downloadButton).setBackgroundColor(color);

    if (!TextUtils.isEmpty(_options.getTitle())) {
      this.setTitle(_options.getTitle());
    } else {
      try {
        URI uri = new URI(_options.getUrl());
        this.setTitle(uri.getHost());
      } catch (URISyntaxException e) {
        this.setTitle(_options.getTitle());
      }
    }

    View backButton = _toolbar.findViewById(R.id.backButton);
    backButton.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            if (_webView.canGoBack()) {
              _webView.goBack();
            }
          }
        });

    View forwardButton = _toolbar.findViewById(R.id.forwardButton);
    forwardButton.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            if (_webView.canGoForward()) {
              Log.d("WebViewDialog", "Go forward");
              _webView.goForward();
            }
          }
        });

    View shareButton = _toolbar.findViewById(R.id.shareButton);
    shareButton.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, _webView.getUrl());
            _context.startActivity(Intent.createChooser(shareIntent, "Share link using"));
            notifyShareButtonClicked();
          }
        });

    View downloadButton = _toolbar.findViewById(R.id.downloadButton);
    downloadButton.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            if (_webView != null) {
              String url = _webView.getUrl();
              if (url != null) {
                PrintManager printManager = (PrintManager) _context.getSystemService(Context.PRINT_SERVICE);
                String jobName = url + " " + System.currentTimeMillis();

                PrintDocumentAdapter printAdapter = _webView.createPrintDocumentAdapter(jobName);
                PrintAttributes.Builder builder = new PrintAttributes.Builder();
                builder.setMediaSize(PrintAttributes.MediaSize.ISO_A4);
                builder.setResolution(new PrintAttributes.Resolution("pdf", "pdf", 600, 600));
                builder.setMinMargins(PrintAttributes.Margins.NO_MARGINS);

                printManager.print(jobName, printAdapter, builder.build());
              }

              notifyDownloadButtonClicked();
            }
          }
        });

    View closeButton = _toolbar.findViewById(R.id.closeButton);
    closeButton.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            // if closeModal true then display a native modal to check if the user is sure
            // to close the browser
            if (_options.getCloseModal()) {
              new AlertDialog.Builder(_context)
                  .setTitle(_options.getCloseModalTitle())
                  .setMessage(_options.getCloseModalDescription())
                  .setPositiveButton(
                      _options.getCloseModalOk(),
                      new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                          // Close button clicked, do something
                          dismiss();
                          _options.getCallbacks().closeEvent(_webView.getUrl());
                          _webView.destroy();
                        }
                      })
                  .setNegativeButton(_options.getCloseModalCancel(), null)
                  .show();
            } else {
              dismiss();
              _options.getCallbacks().closeEvent(_webView.getUrl());
              _webView.destroy();
            }
          }
        });

    if (_options.showArrow()) {
      closeButton.setBackgroundResource(R.drawable.arrow_forward_enabled);
    }

    if (_options.showNavigationButtons()) {
      backButton.setVisibility(View.VISIBLE);
      forwardButton.setVisibility(View.VISIBLE);
    }

    if (_options.showShareButton()) {
      shareButton.setVisibility(View.VISIBLE);
    }

    if (_options.showDownloadButton()) {
      downloadButton.setVisibility(View.VISIBLE);
    }

    if (_options.getShowReloadButton()) {
      View reloadButton = _toolbar.findViewById(R.id.reloadButton);
      reloadButton.setVisibility(View.VISIBLE);
      reloadButton.setOnClickListener(
          new View.OnClickListener() {
            @Override
            public void onClick(View view) {
              _webView.reload();
            }
          });
    }

    if (TextUtils.equals(_options.getToolbarType(), "activity")) {
      _toolbar.findViewById(R.id.forwardButton).setVisibility(View.GONE);
      _toolbar.findViewById(R.id.backButton).setVisibility(View.GONE);
      _toolbar.findViewById(R.id.shareButton).setVisibility(View.GONE);
      _toolbar.findViewById(R.id.downloadButton).setVisibility(View.GONE);
      // TODO: Add share button functionality
    } else if (TextUtils.equals(_options.getToolbarType(), "navigation")) {
      // TODO: Remove share button when implemented
    } else if (TextUtils.equals(_options.getToolbarType(), "blank")) {
      _toolbar.setVisibility(View.GONE);
    } else {
      _toolbar.findViewById(R.id.forwardButton).setVisibility(View.GONE);
      _toolbar.findViewById(R.id.backButton).setVisibility(View.GONE);
    }
  }

  private void setWebViewClient() {
    _webView.setWebViewClient(
        new WebViewClient() {
          @Override
          public boolean shouldOverrideUrlLoading(
              WebView view,
              WebResourceRequest request) {
            Context context = view.getContext();
            String url = request.getUrl().toString();

            if (!url.startsWith("https://") && !url.startsWith("http://")) {
              try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                return true;
              } catch (ActivityNotFoundException e) {
                // Do nothing
              }
            }
            return false;
          }

          @Override
          public void onLoadResource(WebView view, String url) {
            super.onLoadResource(view, url);
          }

          @Override
          public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            try {
              URI uri = new URI(url);
              if (TextUtils.isEmpty(_options.getTitle())) {
                setTitle(uri.getHost());
              }
            } catch (URISyntaxException e) {
              // Do nothing
            }
          }

          public void doUpdateVisitedHistory(
              WebView view,
              String url,
              boolean isReload) {
            if (!isReload) {
              _options.getCallbacks().urlChangeEvent(url);
            }
            super.doUpdateVisitedHistory(view, url, isReload);
          }

          @Override
          public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            _options.getCallbacks().pageLoaded();
            if (!isInitialized) {
              isInitialized = true;
              _webView.clearHistory();
              if (_options.isPresentAfterPageLoad()) {
                show();
                _options.getPluginCall().resolve();
              }
            }

            ImageButton backButton = _toolbar.findViewById(R.id.backButton);
            if (_webView.canGoBack()) {
              backButton.setImageResource(R.drawable.arrow_back_enabled);
              backButton.setEnabled(true);
            } else {
              backButton.setImageResource(R.drawable.arrow_back_disabled);
              backButton.setEnabled(false);
            }

            ImageButton forwardButton = _toolbar.findViewById(R.id.forwardButton);
            if (_webView.canGoForward()) {
              forwardButton.setImageResource(R.drawable.arrow_forward_enabled);
              forwardButton.setEnabled(true);
            } else {
              forwardButton.setImageResource(R.drawable.arrow_forward_disabled);
              forwardButton.setEnabled(false);
            }

            ImageButton shareButton = _toolbar.findViewById(R.id.shareButton);
            shareButton.setImageResource(R.drawable.share_svg);
            shareButton.setEnabled(true);

            ImageButton downloadButton = _toolbar.findViewById(R.id.downloadButton);
            downloadButton.setEnabled(true);

            ImageButton closeButton = _toolbar.findViewById(R.id.closeButton);
            closeButton.setEnabled(true);

            int paddingRight = 25;
            closeButton.setPadding(closeButton.getPaddingLeft(), closeButton.getPaddingTop(), paddingRight,
                closeButton.getPaddingBottom());

            Bitmap originalBitmap = BitmapFactory.decodeResource(_context.getResources(), R.drawable.download_enabled);
            Bitmap originalBitmapClose = BitmapFactory.decodeResource(_context.getResources(), R.drawable.close);
            Bitmap originalBitmapBack = BitmapFactory.decodeResource(_context.getResources(), R.drawable.prev);
            Bitmap originalBitmapForward = BitmapFactory.decodeResource(_context.getResources(), R.drawable.next);

            int width = 55;
            int height = 55;
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, width, height, false);
            Bitmap resizedBitmapClose = Bitmap.createScaledBitmap(originalBitmapClose, width, height, false);
            Bitmap resizedBitmapBack = Bitmap.createScaledBitmap(originalBitmapBack, width, height, false);
            Bitmap resizedBitmapForward = Bitmap.createScaledBitmap(originalBitmapForward, width, height, false);

            downloadButton.setImageBitmap(resizedBitmap);
            closeButton.setImageBitmap(resizedBitmapClose);
            backButton.setImageBitmap(resizedBitmapBack);
            forwardButton.setImageBitmap(resizedBitmapForward);

            _options.getCallbacks().pageLoaded();
          }

          @Override
          public void onReceivedError(
              WebView view,
              WebResourceRequest request,
              WebResourceError error) {
            super.onReceivedError(view, request, error);
            _options.getCallbacks().pageLoadError();
          }

          @SuppressLint("WebViewClientOnReceivedSslError")
          @Override
          public void onReceivedSslError(
              WebView view,
              SslErrorHandler handler,
              SslError error) {
            boolean ignoreSSLUntrustedError = _options.ignoreUntrustedSSLError();
            if (ignoreSSLUntrustedError &&
                error.getPrimaryError() == SslError.SSL_UNTRUSTED)
              handler.proceed();
            else {
              super.onReceivedSslError(view, handler, error);
            }
          }
        });
  }

  @Override
  public void onBackPressed() {
    if (_webView.canGoBack() &&
        (TextUtils.equals(_options.getToolbarType(), "navigation") ||
            _options.getActiveNativeNavigationForWebview())) {
      _webView.goBack();
    } else if (!_options.getDisableGoBackOnNativeApplication()) {
      super.onBackPressed();
    }
  }

  public void setPosition(String position) {
    this.position = position;
    // Lógica para establecer la posición del WebViewDialog según el valor de
    // `position`
    Window window = getWindow();
    if (window != null) {
      WindowManager.LayoutParams params = window.getAttributes();
      switch (position) {
        case "top":
          params.gravity = Gravity.TOP;
          break;
        case "bottom":
          params.gravity = Gravity.BOTTOM;
          break;
        case "center":
          params.gravity = Gravity.CENTER;
          break;
        default:
          params.gravity = Gravity.TOP; // Posición por defecto
          break;
      }
      window.setAttributes(params);
    }
  }

  public WebView getWebView() {
    return _webView;
  }
}
