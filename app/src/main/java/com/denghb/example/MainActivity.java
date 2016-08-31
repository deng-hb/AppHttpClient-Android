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

    private static String[] data = new String[]{"GET", "POST", "Download"};

    private static final String SERVER_ADDERSS = "http://10.0.2.2:8090";

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
                        client.get(SERVER_ADDERSS + "/", new AppHttpClient.CompletionHandler<String>() {
                            @Override
                            public void response(final AppHttpClient.Response<String> response, Exception e) {

                                System.out.println("GET response code:" + response.getCode());
                                if (null != e) {
                                    MainActivity.this.setTitle("GET ERROR");
                                    e.printStackTrace();
                                } else {
                                    MainActivity.this.setTitle("GET SUCCESS");
                                }
                            }
                        });
                        break;
                    }
                    case 1: {
//                            System.out.println("@@@@@:" + Environment.getExternalStorageDirectory());
//                            File file = new File(Environment.getExternalStorageDirectory() + "/xxx.dmg");
//                            FileInputStream is = new FileInputStream(file);
//
//                            System.out.println(is.available());
//
//                            byte[] bytes = new byte[is.available()];
//                            is.read(bytes);
//                            is.close();

                        Map<String, Object> filemap = new HashMap<String, Object>();
                        filemap.put("file_name", "QQ.png");
//                            filemap.put("file_data", bytes);
                        filemap.put("file_path", Environment.getExternalStorageDirectory() + "/xxx.png");

                        Map<String, Object> parameters = new HashMap<String, Object>();
                        parameters.put("images", filemap);
                        parameters.put("amount", "张三");

                        AppHttpClient client = new AppHttpClient();
                        client.post(SERVER_ADDERSS + "/upload", parameters, new AppHttpClient.CompletionHandler() {
                            @Override
                            public void response(AppHttpClient.Response response, Exception e) {
                                System.out.println("POST response code:" + response.getCode());
                                if (null != e) {
                                    MainActivity.this.setTitle("POST ERROR");
                                    e.printStackTrace();
                                } else {
                                    MainActivity.this.setTitle("POST SUCCESS");
                                }
                            }
                        });

                        break;
                    }
                    case 2: {
                        AppHttpClient client = new AppHttpClient();
                        client.download(SERVER_ADDERSS + "/assets/alipay.png", Environment.getExternalStorageDirectory() + "/xxx.png", new AppHttpClient.ProgressHandler() {
                            @Override
                            public void progress(double progress) {
                                MainActivity.this.setTitle("Download Progress:" + (int) progress + "%");
                                System.out.println("++++++++++++++++++progress:" + progress);
                            }
                        }, new AppHttpClient.CompletionHandler() {
                            @Override
                            public void response(AppHttpClient.Response response, Exception e) {

                                if (null != e) {
                                    MainActivity.this.setTitle("Download ERROR");
                                    e.printStackTrace();
                                } else {
                                    MainActivity.this.setTitle("Download SUCCESS");
                                }
                                System.out.println("++++++++++++++++++response code:" + response.getCode());
                            }
                        });

                        break;
                    }
                }
            }
        });

    }


}
