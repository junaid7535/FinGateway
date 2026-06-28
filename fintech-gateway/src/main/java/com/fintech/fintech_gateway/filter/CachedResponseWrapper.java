package com.fintech.fintech_gateway.filter;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.*;

public class CachedResponseWrapper extends HttpServletResponseWrapper {

    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    private final ServletOutputStream outputStream = new ServletOutputStream() {
        public void write(int b) { buffer.write(b); }
        public boolean isReady() { return true; }
        public void setWriteListener(WriteListener l) {}
    };

    private final PrintWriter writer = new PrintWriter(
            new OutputStreamWriter(buffer));

    public CachedResponseWrapper(HttpServletResponse response) {
        super(response);
        // Remove chunked transfer encoding — we set content length explicitly
        response.setHeader("Transfer-Encoding", null);
    }

    @Override
    public PrintWriter getWriter() { return writer; }

    @Override
    public ServletOutputStream getOutputStream() { return outputStream; }

    public String getCapturedBody() {
        writer.flush();
        return buffer.toString();
    }

    public void copyBodyToResponse() throws IOException {
        writer.flush();
        byte[] body = buffer.toByteArray();
        // Set explicit content length to prevent chunked encoding
        getResponse().setContentLength(body.length);
        getResponse().getOutputStream().write(body);
        getResponse().getOutputStream().flush();
    }
}