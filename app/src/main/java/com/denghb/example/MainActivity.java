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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 *
 */
public class MainActivity extends Activity {

    private ListView mList;
    private TextAdapter mAdapter;

    private static String[] data = new String[]{"GET", "POST", "Upload", "Uploads", "Download"};

    private static final String SERVER_ADDERSS = "http://denghb.com";

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
                        Map<String, Object> parameters = new HashMap<String, Object>();
                        parameters.put("amount", "1000");

                        AppHttpClient client = new AppHttpClient();
                        client.post(SERVER_ADDERSS + "/post", parameters, new AppHttpClient.CompletionHandler() {
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
                        Map<String, Object> filemap = new HashMap<String, Object>();
                        filemap.put("file_path", Environment.getExternalStorageDirectory() + "/alipay.png");

                        Map<String, Object> parameters = new HashMap<String, Object>();
                        parameters.put("images", filemap);
                        parameters.put("amount", "10");

                        AppHttpClient client = new AppHttpClient();
                        client.post(SERVER_ADDERSS + "/upload", parameters, new AppHttpClient.CompletionHandler() {
                            @Override
                            public void response(AppHttpClient.Response response, Exception e) {
                                System.out.println("Upload response code:" + response.getCode());
                                if (null != e) {
                                    MainActivity.this.setTitle("Upload ERROR");
                                    e.printStackTrace();
                                } else {
                                    MainActivity.this.setTitle("Upload SUCCESS");
                                }
                            }
                        });

                        break;
                    }
                    case 3: {
                        List<Map<String,Object>> list = new ArrayList<Map<String,Object>>();
                        Map<String, Object> filemap = new HashMap<String, Object>();
                        try {
                            File file = new File(Environment.getExternalStorageDirectory() + "/alipay.png");
                            FileInputStream is = new FileInputStream(file);

                            System.out.println(is.available());

                            byte[] bytes = new byte[is.available()];
                            is.read(bytes);

                            is.close();


                            filemap.put("file_name", "QQ.png");
                            filemap.put("file_data", bytes);

                            list.add(filemap);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        Map<String, Object> filemap2 = new HashMap<String, Object>();
                        filemap2.put("file_path", Environment.getExternalStorageDirectory() + "/alipay.png");
                        list.add(filemap2);

                        Map<String, Object> parameters = new HashMap<String, Object>();
                        parameters.put("images", list);
                        parameters.put("amount", "100");

                        AppHttpClient client = new AppHttpClient();
                        client.post(SERVER_ADDERSS + "/upload", parameters, new AppHttpClient.CompletionHandler() {
                            @Override
                            public void response(AppHttpClient.Response response, Exception e) {
                                System.out.println("Uploads response code:" + response.getCode());
                                if (null != e) {
                                    MainActivity.this.setTitle("Uploads ERROR");
                                    e.printStackTrace();
                                } else {
                                    MainActivity.this.setTitle("Uploads SUCCESS");
                                }
                            }
                        });

                        break;
                    }
                    case 4: {
                        AppHttpClient client = new AppHttpClient();
                        client.download(SERVER_ADDERSS + "/assets/alipay.png", Environment.getExternalStorageDirectory() + "/alipay.png", new AppHttpClient.ProgressHandler() {
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
                                System.out.println("Download response code:" + response.getCode());
                            }
                        });

                        break;
                    }
                }
            }
        });

    }


}
