package org.example.controller;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

public class TestHttpConnection {

    public static void main(String[] args) {
        try {
            URL url = new URL("http://localhost:8080/");
            HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
            httpCon.setDoOutput(true);
            httpCon.setRequestMethod("POST");
            OutputStream os = httpCon.getOutputStream();
            OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
            osw.write("Just Some Text");
            osw.flush();
            osw.close();
            os.close();  //don't forget to close the OutputStream
            httpCon.connect();

//read the inputstream and print it
            String result;
            BufferedInputStream bis = new BufferedInputStream(httpCon.getInputStream());
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            int result2 = bis.read();
            while (result2 != -1) {
                buf.write((byte) result2);
                result2 = bis.read();
            }
            result = buf.toString();
            System.out.println(result);

        } catch (IOException e) {

        }
    }

}
