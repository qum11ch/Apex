package com.example.f1app;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class GetRaceData extends Thread{
    private String url, method, username;
    JSONArray result_data;
    String[] data, field;

    public GetRaceData(String url, String method, String username) {
        this.url = url;
        this.method = method;
        this.username = username;
    }

    @Override
    public void run() {
        try {
            String UTF8 = "UTF-8", iso = "iso-8859-1";
            URL url = new URL(this.url);
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestMethod(this.method);
            httpURLConnection.setDoOutput(true);
            httpURLConnection.setDoInput(true);
            OutputStream outputStream = httpURLConnection.getOutputStream();
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream, UTF8));
            StringBuilder post_data = new StringBuilder();
            post_data.append(URLEncoder.encode("username", "UTF-8")).
                    append("=").append(URLEncoder.encode(username, UTF8)).append("&");
            bufferedWriter.write(post_data.toString());
            bufferedWriter.flush();
            bufferedWriter.close();
            outputStream.close();
            InputStream inputStream = httpURLConnection.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, iso));
            StringBuilder result = new StringBuilder();
            String result_line;
            while ((result_line = bufferedReader.readLine()) != null) {
                result.append(result_line);
            }
            bufferedReader.close();
            inputStream.close();
            Log.i("check result", String.valueOf(result));
            JSONArray myObject = new JSONArray(String.valueOf(result));
            httpURLConnection.disconnect();
            setData(myObject);
        } catch (JSONException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean startPut() {
        GetRaceData.this.start();
        return true;
    }
    public boolean onComplete() {
        while (true) {
            if (!this.isAlive()) {
                return true;
            }
        }
    }
    public JSONArray getResult() {
        return this.getData();
    }
    public void setData(JSONArray result_data) {
        this.result_data = result_data;
    }
    public JSONArray getData() {
        return result_data;
    }
}
