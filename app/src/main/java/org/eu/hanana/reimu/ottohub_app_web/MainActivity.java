package org.eu.hanana.reimu.ottohub_app_web;

import static org.eu.hanana.reimu.ottohub_app_web.Utils.getMimeTypeFromExtension;

import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.PreferenceManager;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.eu.hanana.reimu.ottohub_app_web.frag.SettingsFragment;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    public static final String INTERNAL_URL = "https://app.files"; //虚拟网址起始值，用于加载本地资源
    public static final String ARG_URL = "url"; //虚拟网址起始值，用于加载本地资源
    public String url = "https://ottohub.cn/";
    public MaterialToolbar toolbar;
    public WebView webView;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private View customView;
    private ProgressBar progressBar;
    private ValueCallback<Uri[]> filePathCallback;
    private ActivityResultLauncher<Intent> fileChooserLauncher;
    public boolean rootActivity = true;
    private Menu mainMenu;
    private int shouldClearHistory = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        //加载参数
        if (getIntent().hasExtra(ARG_URL)) {
            url = getIntent().getStringExtra(ARG_URL);
            rootActivity = false;
        }
        WebView.setWebContentsDebuggingEnabled(true);
        toolbar = findViewById(R.id.topAppBar);
        progressBar = findViewById(R.id.progressBar);
        setSupportActionBar(toolbar);
        setTitle(R.string.app_name);
        webView = findViewById(R.id.webview);
        // 启用返回按钮
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (webView.canGoBack()&&!webView.getUrl().endsWith("ottohub.cn/")&&!webView.getUrl().endsWith("ottohub.cn")) {
                    webView.goBack();
                } else {
                    if (rootActivity) {
                        new MaterialAlertDialogBuilder(MainActivity.this)
                                .setTitle(R.string.exit)
                                .setMessage(R.string.exit_msg)
                                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                                    // 当 WebView 无法后退时，调用系统默认返回行为
                                    finish();
                                })
                                .setNegativeButton(R.string.cancel, null)
                                .show();

                    }else {
                        setEnabled(false);
                        getOnBackPressedDispatcher().onBackPressed();
                    }
                }
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            settings.setAlgorithmicDarkeningAllowed(true);
        }
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setDomStorageEnabled(true);
        settings.setSupportZoom(true);
        settings.setSupportMultipleWindows(true);
        settings.setGeolocationEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (shouldClearHistory>0) {
                    view.clearHistory(); // 清空后退栈
                    shouldClearHistory --;
                }
                String scriptUrl = "https://app.files/injector.js";//app内的虚拟文件地址（对应assets/web/injector.js）
                String js = ""
                        + "var script = document.createElement('script');"
                        + "script.src = '" + scriptUrl + "';"
                        + "script.type = 'text/javascript';"
                        + "script.async = false;"
                        + "document.head.appendChild(script);";
                if (PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getBoolean("inject_js",true)) {
                    view.evaluateJavascript(js, null);
                }
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();

                if (uri.toString().toLowerCase(Locale.ROOT).startsWith(MainActivity.INTERNAL_URL)) {
                    String assetPath = uri.getPath(); // 例如 /demo/test.js
                    if (assetPath != null) {
                        try {
                            // 去掉开头的 `/`
                            InputStream input = getAssets().open("web/"+assetPath.substring(1));
                            String mimeType = getMimeTypeFromExtension(assetPath);
                            return new WebResourceResponse(mimeType, "utf-8", input);
                        } catch (IOException e) {
                            return null; // 资源不存在时返回默认处理
                        }
                    }
                }

                return super.shouldInterceptRequest(view, request);
            }
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                var url = request.getUrl().toString();
                return this.shouldOverrideUrlLoading(view,url);
            }

            // 对于 Android 5 以下兼容
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    view.loadUrl(url);
                }else {
                    try {
                        Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                        if (intent != null) {
                            PackageManager pm = view.getContext().getPackageManager();
                            // 查询能够处理该 Intent 的应用
                            ResolveInfo resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);

                            String appName = "Unknown App";
                            if (resolveInfo != null) {
                                CharSequence label = resolveInfo.loadLabel(pm);
                                if (label != null) appName = label.toString();
                            }
                            if (!appName.equals("Unknown App")) {
                                // 弹窗提示打开的应用名
                                new MaterialAlertDialogBuilder(view.getContext())
                                        .setTitle(R.string.open_ext)
                                        .setMessage(getString(R.string.open_ext_msg, appName))
                                        .setPositiveButton(R.string.confirm, (dialog, which) -> {
                                            try {
                                                view.getContext().startActivity(intent);
                                            } catch (Exception e) {
                                                Toast.makeText(view.getContext(), e.toString(), Toast.LENGTH_SHORT).show();
                                            }
                                        })
                                        .setNegativeButton(R.string.cancel, null)
                                        .show();
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return true; // ✅ 告诉系统“我已经处理了”，别跳浏览器
            }

        });
        webView.setWebChromeClient(new WebChromeClient(){
            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
                // 得到网页标题，通常用来设置 Toolbar 标题
                toolbar.setSubtitle(title);
            }
            // 进入全屏
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (customView != null) {
                    callback.onCustomViewHidden();
                    return;
                }
                customView = view;
                customViewCallback = callback;

                ViewGroup root = (ViewGroup) getWindow().getDecorView();
                root.addView(customView, new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));

                // 隐藏 WebView
                webView.setVisibility(View.GONE);

                // 设置 Activity 全屏
                getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            }
            // 退出全屏
            @Override
            public void onHideCustomView() {
                if (customView == null) {
                    return;
                }

                ViewGroup root = (ViewGroup) getWindow().getDecorView();
                root.removeView(customView);
                customView = null;

                // 恢复 WebView 显示
                webView.setVisibility(View.VISIBLE);

                // 退出全屏显示
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);

                if (customViewCallback != null) {
                    customViewCallback.onCustomViewHidden();
                    customViewCallback = null;
                }
            }
            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                new MaterialAlertDialogBuilder(MainActivity.this)
                        .setTitle(R.string.web_page_info)
                        .setMessage(message)
                        .setPositiveButton(R.string.confirm, (dialog, which) -> result.confirm())
                        .setOnCancelListener(dialog -> result.cancel())
                        .show();
                return true; // 表示你已经处理了这个 alert
            }
            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                // 创建一个占位的 WebView，系统会把请求的 URL 填进来
                WebView newWebView = new WebView(MainActivity.this);

                // 拿到 transport 传给系统
                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(newWebView);
                resultMsg.sendToTarget();

                // 拦截 URL 加载
                newWebView.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        // ✅ 打开新 Activity 加载网页
                        Intent intent = new Intent(MainActivity.this, MainActivity.class);
                        intent.putExtra("url", url);
                        startActivity(intent);
                        return true; // 我们自己处理了
                    }
                });

                return true;
            }

            @Override
            public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
                new MaterialAlertDialogBuilder(MainActivity.this)
                        .setTitle(R.string.web_page_info)
                        .setMessage(message)
                        .setPositiveButton(R.string.confirm, (dialog, which) -> result.confirm())
                        .setNegativeButton(R.string.cancel, (dialog, which) -> result.cancel())
                        .setOnCancelListener(dialog -> result.cancel())
                        .show();
                return true; // 表示你已经处理了这个 alert
            }

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress < 100) {
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setAlpha(1);
                    progressBar.setProgress(newProgress);
                    toolbar.setSubtitle(R.string.loading);
                } else {
                    progressBar.setProgress(100);
                    progressBar.animate()
                            .alpha(0f)
                            .setDuration(600)
                            .withEndAction(() -> {
                                progressBar.setVisibility(View.GONE);
                                progressBar.setAlpha(1f);
                            });
                    toolbar.setSubtitle(view.getTitle());
                }
            }
            @Override
            public boolean onShowFileChooser(WebView webView,
                                             ValueCallback<Uri[]> filePathCallback,
                                             FileChooserParams fileChooserParams) {
                MainActivity.this.filePathCallback = filePathCallback;

                Intent intent = fileChooserParams.createIntent();
                try {
                    fileChooserLauncher.launch(intent);
                } catch (ActivityNotFoundException e) {
                    new MaterialAlertDialogBuilder(MainActivity.this)
                            .setTitle(R.string.unable_open_file_chooser )
                            .setMessage(e.toString())
                            .show();
                    return false;
                }
                return true;
            }
        });

        fileChooserLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (filePathCallback != null) {
                        Uri[] results = null;
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Uri data = result.getData().getData();
                            if (data != null) {
                                results = new Uri[]{data};
                            }
                        }
                        filePathCallback.onReceiveValue(results);
                        filePathCallback = null;
                    }
                }
        );
        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            String fileName = URLUtil.guessFileName(url, contentDisposition, mimeType);

            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.download)
                    .setMessage(getString(R.string.download_msg) + fileName)
                    .setPositiveButton(R.string.download, (dialog, which) -> {
                        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                        request.setMimeType(mimeType);
                        request.addRequestHeader("User-Agent", userAgent);
                        request.setTitle(fileName);
                        request.setDescription(getString(R.string.downloading) + fileName);
                        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

                        DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                        dm.enqueue(request);

                        Toast.makeText(this, R.string.downloading, Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        });
        webView.addJavascriptInterface(new JsInterface(),"ohapp");
        //webView.addJavascriptInterface(new JavaJsInterface(),"Java");
        webView.loadUrl(url);
        check();
    }

    private void check() {
        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean first = defaultSharedPreferences.getBoolean("first", true);
        if (first){
            new MaterialAlertDialogBuilder(MainActivity.this)
                    .setTitle(R.string.welcome)
                    .setMessage(R.string.welcome_msg)
                    .setPositiveButton(R.string.confirm, (dialog, which) -> {
                        webView.loadUrl("https://m.ottohub.cn/b/20608");
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        }
        defaultSharedPreferences.edit().putBoolean("first",false).apply();
    }

    protected class JsInterface{
        @JavascriptInterface
        public void setSearchEnable(boolean enable){
            runOnUiThread(()->mainMenu.findItem(R.id.menu_btn_search).setVisible(enable));
        }
        @JavascriptInterface
        public void setHomeEnable(boolean enable){
            runOnUiThread(()->mainMenu.findItem(R.id.menu_btn_home).setVisible(enable));
        }
        @JavascriptInterface
        public void toast(String s){
            runOnUiThread(()->{
                Toast.makeText(MainActivity.this, s, Toast.LENGTH_SHORT).show();
            });
        }
        @JavascriptInterface
        public String getLanguage(String s){
            Locale locale = Resources.getSystem().getConfiguration().locale;
            String language = locale.getLanguage();  // 语言代码，比如 "zh"
            return language;
        }
        @JavascriptInterface
        public void setProgress(int v){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                runOnUiThread(()->webView.getWebChromeClient().onProgressChanged(webView,v));
            }
        }
        @JavascriptInterface
        public void injectJs(String scriptUrl){
            String js = ""
                    + "var script = document.createElement('script');"
                    + "script.src = '" + scriptUrl + "';"
                    + "script.type = 'text/javascript';"
                    + "script.async = false;"
                    + "document.head.appendChild(script);";
            runOnUiThread(()->{
                webView.evaluateJavascript(js, null);
            });
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        this.mainMenu = menu; // 保存 menu 引用
        mainMenu.findItem(R.id.menu_btn_search).setVisible(false);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_exit) {
            new MaterialAlertDialogBuilder(MainActivity.this)
                    .setTitle(R.string.exit)
                    .setMessage(R.string.exit_msg)
                    .setPositiveButton(R.string.confirm, (dialog, which) -> finish())
                    .setNegativeButton(R.string.cancel, null)
                    .show();
            return true;
        } else if (id == R.id.menu_btn_copy_link) {
            String url = webView.getUrl();
            if (url != null && !url.isEmpty()) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Link", url);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Empty", Toast.LENGTH_SHORT).show();
            }
            return true;
        } else if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        } else if (item.getItemId() == R.id.menu_btn_open) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(webView.getUrl()));
            this.startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.menu_btn_search) {
            webView.evaluateJavascript("$(\"#search\").click();",null);
            return true;
        } else if (item.getItemId() == R.id.menu_btn_home) {
            // 延迟清空历史（确保页面加载后执行）
            loadAndClearHistory("https://ottohub.cn/");
            return true;
        } else if (item.getItemId() == R.id.btn_about) {
            Intent intent = new Intent(this, AboutActivity.class);
            this.startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.menu_settings) {
            Intent intent = FragActivity.getIntent(this, null, SettingsFragment.class, getString(R.string.settings));
            this.startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.menu_help) {
            webView.loadUrl("https://m.ottohub.cn/b/20608");
            return true;
        } else if (item.getItemId() == R.id.menu_plugin) {
            webView.loadUrl("https://app.files/webui/plugin/index.html");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
    // 调用该方法跳转并清空历史
    public void loadAndClearHistory(String targetUrl) {
        shouldClearHistory = 2;
        webView.loadUrl(targetUrl);
    }

}