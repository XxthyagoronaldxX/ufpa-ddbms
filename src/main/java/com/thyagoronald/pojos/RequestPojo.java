package com.thyagoronald.pojos;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Objects;

import lombok.Data;

@Data
public class RequestPojo implements Closeable {
    private String protocolVersion;
    private String method;
    private String host;
    private long crc;
    private String content;
    private BufferedReader in;

    public RequestPojo(Socket socket) throws IOException {
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.host = socket.getInetAddress().getHostAddress();

        String header = in.readLine();
        String[] headerParts = header.split(" ", 2);
        this.protocolVersion = headerParts[0];
        this.method = headerParts[1];

        String crcLine = in.readLine();
        this.crc = Long.parseLong(crcLine.split(" ", 2)[1]);

        String contentLine = in.readLine();
        this.content = contentLine.split(" ", 2)[1];
    }

    public String readLine() throws IOException {
        return in.readLine();
    }

    @Override
    public void close() throws IOException {
        if (Objects.nonNull(in)) {
            in.close();
        }
    }
}
