package com.denghb.example;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.denghb.apphttpclient.AppHttpClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
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
                switch (position) {
                    case 0: {
                        AppHttpClient client = new AppHttpClient();
                        client.get("https://denghb.com", new AppHttpClient.CompletionHandler<String>() {
                            @Override
                            public void response(final AppHttpClient.Response<String> response, Exception e) {
                                MainActivity.this.setTitle("GET SUCCESS");
                                System.out.println("GET response code:" + response.getCode());

                            }
                        });
                        break;
                    }
                    case 1: {
                        try {
                            System.out.println("@@@@@:" + Environment.getExternalStorageDirectory());
                            File file = new File(Environment.getExternalStorageDirectory(), "/DCIM/100ANDRO/QQ20160519-0@2x.png");
                            FileInputStream is = new FileInputStream(file);
                            byte[] bytes = new byte[is.available()];
                            is.read(bytes);
                            is.close();

                            Map<String, Object> filemap = new HashMap<String, Object>();
                            filemap.put("file_name", "QQ.png");
                            filemap.put("file_data", bytes);

                            Map<String, Object> parameters = new HashMap<String, Object>();
                            parameters.put("file", filemap);
                            parameters.put("name", "张三");

                            AppHttpClient client = new AppHttpClient();
                            client.post("https://denghb.com/", parameters, new AppHttpClient.CompletionHandler() {
                                @Override
                                public void response(AppHttpClient.Response response, Exception e) {
                                    System.out.println("POST response code:" + response.getCode());
                                }
                            });

                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                    }
                }
            }
        });

    }


}
