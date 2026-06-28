package com.fintech.fintech_gateway.filter;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.*;

public class CachedBodyRequestWrapper extends HttpServletRequestWrapper {

    private final byte[] cachedBody;

    public CachedBodyRequestWrapper(HttpServletRequest request, String body) {
        super(request);
        this.cachedBody = body.getBytes();
    }

    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream byteStream = new ByteArrayInputStream(cachedBody);
        return new ServletInputStream() {
            public int read() { return byteStream.read(); }
            public boolean isFinished() { return byteStream.available() == 0; }
            public boolean isReady() { return true; }
            public void setReadListener(ReadListener l) {}
        };
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream()));
    }
}