package com.denghb.example;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import com.denghb.apphttpclient.AppHttpClient;

import org.w3c.dom.Text;

import java.io.IOException;
import java.util.Map;

/**
 *
 */
public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final TextView txContent = (TextView)findViewById(R.id.content);

        AppHttpClient client = new AppHttpClient();
        client.get("http://jianshu.com", new AppHttpClient.CompletionHandler() {
            @Override
            public void response(final AppHttpClient.Response response, Exception e) {

                txContent.setText((String)response.getBody());

                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

//                        txContent.setText((String)response.getBody());

                    }
                });
            }
        });



    }
}
