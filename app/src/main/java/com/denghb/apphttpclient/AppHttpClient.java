package com.denghb.apphttpclient;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Created by denghb on 16/8/29.
 */
public class AppHttpClient {

    public class Response<T> {
        private int code;

        private T body;

        private Map<String, String> header;

        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public T getBody() {
            return body;
        }

        public void setBody(T body) {
            this.body = body;
        }

        public Map<String, String> getHeader() {
            return header;
        }

        public void setHeader(Map<String, String> header) {
            this.header = header;
        }
    }

    public interface CompletionHandler<T> {
        public void response(Response<T> response, Exception e);
    }

    private static Handler mHander = new Handler();

    private void execute(final String url, final String method, final Map<String, String> parameters, final CompletionHandler handler) {
        boolean isMainThread = Looper.myLooper() != Looper.getMainLooper();

        // 主线程
        ExecutorService service = Executors.newFixedThreadPool(2);

        service.submit(new Runnable() {
            @Override
            public void run() {

                final Response response = new Response();
                final Exception exception = null;
                HttpURLConnection connection = null;
                try {

                    connection = getHttpConnection(url, method);
                    connection.connect();


                    InputStream inputStream = null;
                    if (!TextUtils.isEmpty(connection.getContentEncoding())) {
                        String encode = connection.getContentEncoding().toLowerCase();
                        if (!TextUtils.isEmpty(encode) && encode.indexOf("gzip") >= 0) {
                            inputStream = new GZIPInputStream(connection.getInputStream());
                        }
                    }

                    if (null == inputStream) {
                        inputStream = connection.getInputStream();
                    }

                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                    StringBuilder builder = new StringBuilder();
                    String line = null;
                    while ((line = reader.readLine()) != null) {
                        builder.append(line).append("\n");
                    }

                    response.setBody(builder.toString());
                    response.setCode(connection.getResponseCode());
                    response.setHeader(getHttpResponseHeader(connection));


                } catch (Exception e) {

                } finally {
                    if (null != connection) {
                        connection.disconnect();
                    }
                }

                mHander.post(new Runnable() {
                    @Override
                    public void run() {
                        handler.response(response, exception);
                    }
                });
            }
        });

    }

    public void get(String url, CompletionHandler handler) {
        execute(url, "GET", null, handler);
    }


    private HttpURLConnection getHttpConnection(String urlString, String method) throws IOException {
        URL url = new URL(urlString);

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(30000);
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setUseCaches(false);
        // 请求头
        connection.setRequestProperty("Connection", "Keep-Alive");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 6.1; zh-CN; rv:1.9.2.6)");
        connection.setRequestProperty("Charset", "UTF-8");

        connection.setRequestMethod(method);

        if (connection instanceof HttpsURLConnection) {
            ((HttpsURLConnection) connection).setSSLSocketFactory(getTrustAllSSLSocketFactory());
        }
        return connection;
    }

    public static SSLSocketFactory getTrustAllSSLSocketFactory() {
        // 信任所有证书
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    @Override
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }};
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, null);
            return sslContext.getSocketFactory();
        } catch (Throwable ex) {

        }
        return null;
    }

    private static Map<String, String> getHttpResponseHeader(HttpURLConnection http) {
        Map<String, String> header = new LinkedHashMap<String, String>();
        for (int i = 0; ; i++) {
            String mine = http.getHeaderField(i);
            if (mine == null)
                break;
            header.put(http.getHeaderFieldKey(i), mine);
        }
        return header;
    }
}
