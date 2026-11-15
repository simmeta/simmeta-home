package com.simmeta.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.GeolocationPermissions;
import android.webkit.JsResult;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class MainActivity extends AppCompatActivity {

    public static final String TARGET_URL = "https://simmet-home.netlify.app";

    private WebView webView;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefreshLayout;
    private View layoutError;
    private FrameLayout layoutSplash;
    private Button btnRetry;

    private ValueCallback<Uri[]> fileUploadCallback;
    private long backPressedTime = 0;
    private boolean isInitialLoaded = false;

    // File chooser launcher for <input type="file">
    private final ActivityResultLauncher<Intent> fileChooserLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (fileUploadCallback == null) return;
                Uri[] results = null;
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    if (data.getClipData() != null) {
                        int count = data.getClipData().getItemCount();
                        results = new Uri[count];
                        for (int i = 0; i < count; i++) {
                            results[i] = data.getClipData().getItemAt(i).getUri();
                        }
                    } else if (data.getData() != null) {
                        results = new Uri[]{data.getData()};
                    }
                }
                fileUploadCallback.onReceiveValue(results);
                fileUploadCallback = null;
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Fullscreen & Edge-to-Edge immersion setup
        setupFullscreen();

        setContentView(R.layout.activity_main);

        // 2. View Bindings
        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        layoutError = findViewById(R.id.layoutError);
        layoutSplash = findViewById(R.id.layoutSplash);
        btnRetry = findViewById(R.id.btnRetry);

        // 3. Setup Components
        setupSwipeRefresh();
        setupWebView();
        setupErrorHandling();
        setupBackNavigation();

        // 4. Load initial URL
        if (savedInstanceState == null) {
            loadWebsite(TARGET_URL);
        } else {
            webView.restoreState(savedInstanceState);
        }
    }

    private void setupFullscreen() {
        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);

        int bgColor = Color.parseColor("#060913");
        window.setStatusBarColor(bgColor);
        window.setNavigationBarColor(bgColor);

        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(window, window.getDecorView());
        controller.setAppearanceLightStatusBars(false); // White text/icons on dark status bar
        controller.setAppearanceLightNavigationBars(false);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        webView.setBackgroundColor(Color.parseColor("#060913"));

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        // Custom User-Agent tag
        String defaultUa = settings.getUserAgentString();
        settings.setUserAgentString(defaultUa + " SimMetaApp/1.0.0");

        // Enable Cookies
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);

        // WebViewClient
        webView.setWebViewClient(new SimMetaWebViewClient());

        // WebChromeClient
        webView.setWebChromeClient(new SimMetaWebChromeClient());

        // Download Listener
        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
                try {
                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                    request.setMimeType(mimetype);
                    String cookies = CookieManager.getInstance().getCookie(url);
                    request.addRequestHeader("cookie", cookies);
                    request.addRequestHeader("User-Agent", userAgent);
                    request.setDescription("다운로드 중...");
                    String fileName = URLUtil.guessFileName(url, contentDisposition, mimetype);
                    request.setTitle(fileName);
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

                    DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                    if (dm != null) {
                        dm.enqueue(request);
                        Toast.makeText(MainActivity.this, "다운로드를 시작합니다.", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "다운로드 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeColors(
                Color.parseColor("#6366F1"),
                Color.parseColor("#06B6D4"),
                Color.parseColor("#A855F7")
        );
        swipeRefreshLayout.setProgressBackgroundColorSchemeColor(Color.parseColor("#12182B"));

        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (isNetworkAvailable()) {
                webView.reload();
            } else {
                swipeRefreshLayout.setRefreshing(false);
                showNetworkError();
            }
        });

        // Only allow swipe to refresh when webView is at the top
        webView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            swipeRefreshLayout.setEnabled(scrollY == 0);
        });
    }

    private void setupErrorHandling() {
        btnRetry.setOnClickListener(v -> {
            if (isNetworkAvailable()) {
                hideNetworkError();
                webView.reload();
            } else {
                Toast.makeText(this, "네트워크가 연결되지 않았습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupBackNavigation() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (layoutError.getVisibility() == View.VISIBLE) {
                    finish();
                    return;
                }

                if (webView.canGoBack()) {
                    webView.goBack();
                } else {
                    if (System.currentTimeMillis() - backPressedTime < 2000) {
                        finish();
                    } else {
                        backPressedTime = System.currentTimeMillis();
                        Toast.makeText(MainActivity.this, getString(R.string.back_press_exit), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    private void loadWebsite(String url) {
        if (isNetworkAvailable()) {
            hideNetworkError();
            webView.loadUrl(url);
        } else {
            showNetworkError();
        }
    }

    private void showNetworkError() {
        layoutError.setVisibility(View.VISIBLE);
        webView.setVisibility(View.GONE);
        layoutSplash.setVisibility(View.GONE);
    }

    private void hideNetworkError() {
        layoutError.setVisibility(View.GONE);
        webView.setVisibility(View.VISIBLE);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        Network network = cm.getActiveNetwork();
        if (network == null) return false;

        NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
        return capabilities != null && (
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        );
    }

    // =========================================================================
    // Custom WebViewClient
    // =========================================================================
    private class SimMetaWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();

            if (url.startsWith("http://") || url.startsWith("https://")) {
                Uri uri = Uri.parse(url);
                String host = uri.getHost();

                // Stay inside WebView for app domain or subdomains
                if (host != null && (host.contains("simmet-home.netlify.app") || host.contains("simmeta"))) {
                    return false;
                }

                // Open external links in device browser
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(intent);
                    return true;
                } catch (Exception e) {
                    return false;
                }
            } else {
                // Schemes like tel:, mailto:, sms:, intent:, etc.
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                    return true;
                } catch (Exception e) {
                    return true;
                }
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            swipeRefreshLayout.setRefreshing(false);

            // Hide initial splash screen smoothly on first load
            if (!isInitialLoaded) {
                isInitialLoaded = true;
                layoutSplash.animate()
                        .alpha(0.0f)
                        .setDuration(400)
                        .withEndAction(() -> layoutSplash.setVisibility(View.GONE))
                        .start();
            }
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error);
            if (request.isForMainFrame()) {
                showNetworkError();
            }
        }
    }

    // =========================================================================
    // Custom WebChromeClient
    // =========================================================================
    private class SimMetaWebChromeClient extends WebChromeClient {

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
            if (newProgress < 100) {
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(newProgress);
            } else {
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
            }
        }

        @Override
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
            if (fileUploadCallback != null) {
                fileUploadCallback.onReceiveValue(null);
            }
            fileUploadCallback = filePathCallback;

            Intent intent = fileChooserParams.createIntent();
            try {
                fileChooserLauncher.launch(intent);
            } catch (ActivityNotFoundException e) {
                fileUploadCallback = null;
                Toast.makeText(MainActivity.this, "파일 선택기를 열 수 없습니다.", Toast.LENGTH_SHORT).show();
                return false;
            }
            return true;
        }

        @Override
        public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
            new AlertDialog.Builder(MainActivity.this, R.style.DarkAlertDialogTheme)
                    .setTitle(R.string.app_name)
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> result.confirm())
                    .setCancelable(false)
                    .show();
            return true;
        }

        @Override
        public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
            new AlertDialog.Builder(MainActivity.this, R.style.DarkAlertDialogTheme)
                    .setTitle(R.string.app_name)
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> result.confirm())
                    .setNegativeButton(android.R.string.cancel, (dialog, which) -> result.cancel())
                    .setCancelable(false)
                    .show();
            return true;
        }

        @Override
        public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
            callback.invoke(origin, true, false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) webView.onResume();
    }

    @Override
    protected void onPause() {
        if (webView != null) webView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (webView != null) {
            webView.saveState(outState);
        }
    }
}

