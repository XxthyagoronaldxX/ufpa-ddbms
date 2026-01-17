package com.thyagoronald.pojos;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Objects;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ResponsePojo implements Closeable {
    private PrintWriter out;

    public ResponsePojo(Socket socket) throws IOException {
        this.out = new PrintWriter(socket.getOutputStream(), true);
    }

    public void sendLine(String line) {
        out.println(line);
    }

    @Override
    public void close() throws IOException {
        if (Objects.nonNull(out)) {
            out.close();
        }
    }
}