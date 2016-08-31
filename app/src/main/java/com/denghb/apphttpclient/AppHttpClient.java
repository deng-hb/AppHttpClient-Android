package com.denghb.apphttpclient;

import android.os.Environment;
import android.os.Handler;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.HashMap;
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

    private double progressNumber;
    private Exception exception;

    public interface CompletionHandler<T> {
        public void response(Response<T> response, Exception e);
    }

    public interface ProgressHandler {
        public void progress(double progress);
    }

    private static Handler mHandler = new Handler();

    public void get(String url, CompletionHandler completion) {
        execute(url, "GET", null, null, completion);
    }

    public void post(String url, Map<String, Object> parameters, CompletionHandler completion) {
        execute(url, "POST", parameters, null, completion);
    }

    public void download(String url, String saveAs, ProgressHandler progress, CompletionHandler completion) {

        Map<String, Object> map = new HashMap<>();
        map.put("saveAs", saveAs);
        execute(url, "DOWNLOAD", map, progress, completion);
    }

    private void execute(final String url, final String method, final Map<String, Object> parameters, final ProgressHandler progress, final CompletionHandler completion) {
        // 主线程
        ExecutorService service = Executors.newFixedThreadPool(2);

        if (!"DOWNLOAD".equals(method)) {
            service.submit(new Runnable() {
                @Override
                public void run() {

                    final Response response = new Response();
                    HttpURLConnection connection = null;
                    DataOutputStream output = null;
                    try {

                        connection = getHttpConnection(url, method, parameters);

                        if (null != parameters) {
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
                                        sb.append("\"\r\nContent-Type: text/plain; charset=UTF-8\r\nContent-Transfer-Encoding: 8bit\r\n\r\n");
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
                        }

                        connection.connect();

                        response.setCode(connection.getResponseCode());
                        response.setHeader(getHttpResponseHeader(connection));

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

                    } catch (Exception e) {
                        exception = e;
                    } finally {
                        if (null != connection) {
                            connection.disconnect();
                            connection = null;
                        }
                        if (null != output) {
                            try {
                                output.close();
                            } catch (IOException e) {

                            }
                        }
                    }

                    // TODO 需要弱引用
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (null != completion) {
                                completion.response(response, exception);
                            }
                        }
                    });
                }
            });
        } else {

            service.submit(new Runnable() {
                @Override
                public void run() {

                    // 返回进度信息
                    Runnable progressRunnable = new Runnable() {
                        @Override
                        public void run() {
                            if (null != progress) {
                                progress.progress(progressNumber);
                                mHandler.postDelayed(this, 1000);
                            }
                        }
                    };


                    final Response response = new Response();
                    HttpURLConnection connection = null;
                    OutputStream output = null;
                    try {
                        connection = getHttpConnection(url, method, parameters);

                        connection.connect();

                        response.setCode(connection.getResponseCode());
                        response.setHeader(getHttpResponseHeader(connection));

                        int fileLength = connection.getContentLength();


                        mHandler.postDelayed(progressRunnable, 1000);//每1秒执行一次返回进度
                        InputStream input = connection.getInputStream();

                        output = new FileOutputStream(parameters.get("saveAs").toString());

                        byte data[] = new byte[1024];
                        long total = 0;
                        int count;
                        while ((count = input.read(data)) != -1) {
                            output.write(data, 0, count);
                            total += count;
                            progressNumber = total * 100.0 / fileLength;
                        }
                        output.flush();
                        input.close();


                    } catch (Exception e) {
                        exception = e;
                    } finally {
                        if (null != connection) {
                            connection.disconnect();
                            connection = null;
                        }
                    }


                    // TODO 需要弱引用
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (null != completion) {
                                completion.response(response, exception);
                            }
                        }
                    });
                    // 移除进度
                    mHandler.removeCallbacks(progressRunnable);
                }
            });
        }
    }


    private HttpURLConnection getHttpConnection(String urlString, String method, Map<String, Object> parameters) throws IOException {
        URL url = new URL(urlString);

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(3000);// 3秒
        connection.setReadTimeout(60000);// 1分钟

        if (!"DOWNLOAD".equalsIgnoreCase(method)) {
            connection.setDoOutput(true);
            connection.setDoInput(true);
        }
        connection.setUseCaches(false);
        // 请求头
        connection.setRequestProperty("Connection", "Keep-Alive");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/52.0.2743.116 Safari/537.36");
        connection.setRequestProperty("Charset", "UTF-8");
        if ("POST".equalsIgnoreCase(method)) {
            connection.setRequestMethod("POST");
        }

        if (connection instanceof HttpsURLConnection) {
            ((HttpsURLConnection) connection).setSSLSocketFactory(getTrustAllSSLSocketFactory());
        }
        return connection;
    }


    private void appendData(Map<String, Object> map, DataOutputStream output, String boundary, String name) throws Exception {
        try {
            byte[] bytes = (byte[]) map.get("file_data");
            String fileName = (String) map.get("file_name");

            if (null == bytes) {
                String filePath = (String) map.get("file_path");
                File file = new File(filePath);
                FileInputStream is = new FileInputStream(file);

                bytes = new byte[is.available()];
                is.read(bytes);
                is.close();
            }

            StringBuilder split = new StringBuilder();
            split.append(boundary);
            split.append(String.format("\r\nContent-Disposition: form-data; name=\"%s\"; filename=\"%s\"", name, fileName));
            split.append("\r\nContent-Type: application/octet-stream\r\nContent-Transfer-Encoding: binary\r\n\r\n");

            output.writeBytes(split.toString());
            output.write(bytes);
            output.writeBytes("\r\n");
        } catch (Exception e) {
            throw e;
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
