package com.denghb.apphttpclient;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

    private void execute(final String url, final String method, final Map<String, Object> parameters, final CompletionHandler handler) {
        boolean isMainThread = Looper.myLooper() != Looper.getMainLooper();

        // 主线程
        ExecutorService service = Executors.newFixedThreadPool(2);

        service.submit(new Runnable() {
            @Override
            public void run() {

                final Response response = new Response();
                final Exception exception = null;
                HttpURLConnection connection = null;
                DataOutputStream output = null;
                try {

                    connection = getHttpConnection(url, method, parameters);
                    // 判断是否是文件流
                    boolean isMultipart = false;
                    for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                        Object object = entry.getValue();
                        if (object instanceof Map || object instanceof List) {
                            isMultipart = true;
                            break;
                        }
                    }

                    if (isMultipart) {
                        String boundary = "AppHttpClinet-denghb-com";
                        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                        output = new DataOutputStream(connection.getOutputStream());


                        boundary = "--" + boundary;
                        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                            String key = entry.getKey();
                            Object value = entry.getValue();

                            if (value instanceof List) {
                                List<Map<String, Object>> list = (List<Map<String, Object>>) value;
                                for (Map<String, Object> map : list) {
                                    appendData(map, output, boundary, key);
                                }

                            } else if (value instanceof Map) {
                                Map<String, Object> map = (Map) value;
                                appendData(map, output, boundary, key);
                            } else {
                                StringBuilder sb = new StringBuilder();
                                sb.append(boundary);
                                sb.append("\r\nContent-Disposition: form-data; name=\"");
                                sb.append(key);
                                sb.append("\"\r\n");
                                sb.append(value);
                                sb.append("\r\n");
                                output.writeBytes(sb.toString());
                            }

                        }

                        output.writeBytes(boundary + "--\r\n");// 数据结束标志
                        output.flush();

                    } else {
                        // 纯文本
                        StringBuilder sb = new StringBuilder();
                        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                            sb.append(entry.getKey());
                            sb.append("=");
                            sb.append(entry.getValue());
                            sb.append("&");
                        }

                        output.writeBytes(sb.toString());
                        output.flush();
                        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    }
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
                    if (null != output) {
                        try {
                            output.close();
                        } catch (IOException e) {

                        }
                    }
                }

                // TODO 需要弱引用
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

    public void post(String url, Map<String, Object> parameters, CompletionHandler handler) {
        execute(url, "POST", parameters, handler);
    }


    private HttpURLConnection getHttpConnection(String urlString, String method, Map<String, Object> parameters) throws IOException {
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


    private void appendData(Map<String, Object> map, DataOutputStream output, String boundary, String name) {

        byte[] bytes = (byte[]) map.get("file_data");
        String fileName = (String) map.get("file_name");

        StringBuilder split = new StringBuilder();
        split.append(boundary);
        split.append(String.format("\r\nContent-Disposition: form-data; name=\"%s\"; filename=\"%s\"", name, fileName));
        split.append("\r\nContent-Type: application/octet-stream\r\n\r\n");
        try {
            output.writeBytes(split.toString());
            output.write(bytes);
            output.writeBytes("\r\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static SSLSocketFactory getTrustAllSSLSocketFactory() {
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
