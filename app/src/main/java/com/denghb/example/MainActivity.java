package com.denghb.example;

import android.app.Activity;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.denghb.apphttpclient.AppHttpClient;

import org.w3c.dom.Text;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class MainActivity extends Activity {

    private ListView mList;
    private TextAdapter mAdapter;

    private static String[] data = new String[]{"GET", "POST"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mList = (ListView) findViewById(R.id.list);

        mAdapter = new TextAdapter(this, data);
        mList.setAdapter(mAdapter);
        mList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (0 == position) {
                    AppHttpClient client = new AppHttpClient();
                    client.get("https://denghb.com", new AppHttpClient.CompletionHandler() {
                        @Override
                        public void response(final AppHttpClient.Response response, Exception e) {
                            MainActivity.this.setTitle("GET SUCCESS");
                            System.out.println("response code:" + response.getCode());

                        }
                    });
                }
            }
        });

    }


}
