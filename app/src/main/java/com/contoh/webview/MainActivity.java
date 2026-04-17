package com.contoh.webview;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {
    
    // ==========================================
    // VARIABEL GENERATOR (JANGAN HAPUS KOMENTAR)
    // ==========================================
    private String targetUrl = "file:///android_asset/index.html"; // [SET_URL]
    private boolean isFullScreen = false; // [SET_FS]
    private boolean hideTitleBar = false; // [SET_HT]
    private boolean allowLongPress = true; // [SET_LP]
    private boolean allowZoomUI = true; // [SET_ZOOM]
    private boolean pcMode = false; // [SET_PC]
    private boolean mediaAutoPlay = true; // [SET_MEDIA]
    private boolean allowCamera = true; // [SET_CAM]
    private boolean allowMicrophone = true; // [SET_MIC]
    // ==========================================

    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;
    private final static int FILE_CHOOSER_RESULT_CODE = 1;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Pengaturan Full Screen
        if (isFullScreen) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Pengaturan Sembunyikan Title Bar (Action Bar)
        if (hideTitleBar && getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        webView = findViewById(R.id.webView);
        
        // Pengaturan Long Press (Mencegah copy/paste text jika false)
        if (!allowLongPress) {
            webView.setOnLongClickListener(v -> true);
            webView.setLongClickable(false);
        }

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true); 
        webSettings.setDatabaseEnabled(true);
        
        // Pengaturan Zoom
        webSettings.setSupportZoom(allowZoomUI);
        webSettings.setBuiltInZoomControls(allowZoomUI);
        webSettings.setDisplayZoomControls(false);

        // Pengaturan Media Auto Play
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            webSettings.setMediaPlaybackRequiresUserGesture(!mediaAutoPlay);
        }

        // Pengaturan PC Mode (Desktop Mode)
        if (pcMode) {
            String desktopUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
            webSettings.setUserAgentString(desktopUserAgent);
            webSettings.setUseWideViewPort(true);
            webSettings.setLoadWithOverviewMode(true);
        }

        // Izin Akses File Lokal (CORS Bypass)
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            webSettings.setAllowUniversalAccessFromFileURLs(true);
            webSettings.setAllowFileAccessFromFileURLs(true);
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        }

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                CookieManager.getInstance().flush(); 
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("file://")) {
                    return false;
                }
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                    return true;
                } catch (Exception e) {
                    return true;
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // Cek izin Kamera & Mic sesuai pengaturan dari web generator
                    boolean needCamera = false;
                    boolean needMic = false;
                    
                    for (String resource : request.getResources()) {
                        if (resource.equals(PermissionRequest.RESOURCE_VIDEO_CAPTURE) && allowCamera) needCamera = true;
                        if (resource.equals(PermissionRequest.RESOURCE_AUDIO_CAPTURE) && allowMicrophone) needMic = true;
                    }

                    if (needCamera && ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, 2);
                    }
                    if (needMic && ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, 3);
                    }
                }
                // Berikan izin otomatis ke WebView jika diaktifkan di form
                runOnUiThread(() -> {
                    if(allowCamera || allowMicrophone) {
                         request.grant(request.getResources());
                    } else {
                         request.deny();
                    }
                });
            }

            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                if (MainActivity.this.filePathCallback != null) {
                    MainActivity.this.filePathCallback.onReceiveValue(null);
                }
                MainActivity.this.filePathCallback = filePathCallback;
                Intent intent = fileChooserParams.createIntent();
                try {
                    startActivityForResult(intent, FILE_CHOOSER_RESULT_CODE);
                } catch (Exception e) {
                    MainActivity.this.filePathCallback = null;
                    return false;
                }
                return true;
            }
        });

        webView.loadUrl(targetUrl);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_RESULT_CODE) {
            if (filePathCallback == null) return;
            filePathCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
            filePathCallback = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
