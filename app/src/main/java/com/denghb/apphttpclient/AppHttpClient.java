package com.denghb.apphttpclient;

import android.content.Context;
import android.os.Looper;
import android.os.MessageQueue;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.zip.GZIPInputStream;

/**
 * Created by ppd on 16/8/29.
 */
public class AppHttpClient {

    private Context mContext;

    public AppHttpClient(Context context){
        mContext = context;
    }

    public interface CompletionHandler{
        public void response(byte[] bytes, Map<String,String> header, IOException e);
    }

    private void execute(final String url,final Map<String,String> parameters,CompletionHandler handler) {
        boolean isMainThread = Looper.myLooper() != Looper.getMainLooper();

        // 主线程
        ExecutorService service = Executors.newFixedThreadPool(2);

        Future<URLConnection> future = service.submit(new Callable<URLConnection>() {
            @Override
            public URLConnection call() throws Exception {

                String content = null;

                HttpURLConnection conn = (HttpURLConnection)getConn(url);

                    conn.connect();
                    if (200 == conn.getResponseCode()) {
                        InputStream inputStream = null;
                        if (!TextUtils.isEmpty(conn.getContentEncoding())) {
                            String encode = conn.getContentEncoding().toLowerCase();
                            if (!TextUtils.isEmpty(encode) && encode.indexOf("gzip") >= 0) {
                                inputStream = new GZIPInputStream(conn.getInputStream());
                            }
                        }

                        if (null == inputStream) {
                            inputStream = conn.getInputStream();
                        }

                        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                        StringBuilder builder = new StringBuilder();
                        String line = null;
                        while ((line = reader.readLine()) != null) {
                            builder.append(line).append("\n");
                        }
                        content = builder.toString();
                        handler.response(content.getBytes(),getHttpResponseHeader(conn),null);
                    }else{
                        handler.response(content.getBytes(),getHttpResponseHeader(conn),null);
                    }

                return null;
            }



        });
        while(!future.isDone()){
            URLConnection conn = null;
            try {
                conn = future.get();
                handler.response(null, getHttpResponseHeader(conn), null);
            }catch (IOException e){

            }finally {

            }

        }

    }

    public void get(String url,CompletionHandler handler){



    }



    private URLConnection getConn(String urlString) throws IOException {
        URL url = new URL(urlString);

        URLConnection conn = url.openConnection();
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(30000);
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setUseCaches(false);
        // 请求头
        conn.setRequestProperty("Connection", "Keep-Alive");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 6.1; zh-CN; rv:1.9.2.6)");
        conn.setRequestProperty("Charset", "UTF-8");

        return conn;
    }
    private static Map<String, String> getHttpResponseHeader(HttpURLConnection http) {
        Map<String, String> header = new LinkedHashMap<String, String>();
        for (int i = 0;; i++) {
            String mine = http.getHeaderField(i);
            if (mine == null)
                break;
            header.put(http.getHeaderFieldKey(i), mine);
        }
        return header;
    }
}
