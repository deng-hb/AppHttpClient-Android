package com.denghb.example;

import android.app.Activity;
import android.os.Bundle;

import com.denghb.example.apphttpclient.AppHttpClient;

import java.io.IOException;
import java.util.Map;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {

            }
        });
        getApplication();
        AppHttpClient client = new AppHttpClient(this);
        client.get("http://denghb.com/", new AppHttpClient.CompletionHandler() {
            @Override
            public void response(byte[] bytes, Map<String, String> header, IOException e) {
                System.out.println(header);
            }
        });

    }
}
