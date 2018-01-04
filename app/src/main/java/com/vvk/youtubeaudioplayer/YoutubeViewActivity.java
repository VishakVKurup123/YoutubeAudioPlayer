package com.vvk.youtubeaudioplayer;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class YoutubeViewActivity extends AppCompatActivity implements PlayListManagerEventsListener{

    private PlayListManager mPlayListManager;
    private WebView mWebView;

    private class MyCustomWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        getApplicationContext();
        Intent intent = getIntent();
        String url = intent.getStringExtra("URL");
        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_youtube_view);

        mWebView = (WebView) findViewById(R.id.webview);
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        mWebView.setWebViewClient(new MyCustomWebViewClient());
        mWebView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        // mWebView.setInitialScale(60);
        mWebView.loadUrl(url);

        mPlayListManager = PlayListManager.getInstance(getApplicationContext());
        mPlayListManager.addPlayListManagerEventsListener(this);
    }

    public void onAddBtnClicked(View view) {
        String url = mWebView.getUrl();
        mPlayListManager.addUrltoList(url);
        Log.v("VVK", "URL "+url);
    }

    public void onBackBtnClicked(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);

    }

    @Override
    public void onSongAdded() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                CharSequence text = "Added to playlist";
                int duration = Toast.LENGTH_SHORT;

                Toast toast = Toast.makeText(getApplicationContext(),text,duration);
                toast.show();
            }
        });

    }
}
