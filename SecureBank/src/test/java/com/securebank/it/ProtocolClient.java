package com.securebank.it;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Minimal protocol client for integration tests — one socket, one request/response.
 * Mirrors how BankClient talks to the server: "CMD|p1|p2" in, "OK|..." or "ERROR|..." out.
 */
public class ProtocolClient implements Closeable {

    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;

    public ProtocolClient(String host, int port) throws IOException {
        this.socket = new Socket(host, port);
        this.in = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.out = new PrintWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
    }

    /**
     * Sends one request line and reads one response line.
     *
     * @param request the raw request, e.g. "LOGIN|CUSTOMER-1|1234"
     * @return the raw response, e.g. "OK|Divyansh Kashiv|ACC-001001,ACC-001002"
     */
    public String send(String request) throws IOException {
        out.println(request);
        String response = in.readLine();
        if (response == null) {
            throw new IOException("Server closed the connection — no response to: " + request);
        }
        return response;
    }

    /**
     * Sends a request and asserts the response starts with "OK|".
     *
     * @return everything after "OK|"
     */
    public String sendOk(String request) throws IOException {
        String response = send(request);
        if (!response.startsWith("OK|")) {
            throw new AssertionError("Expected OK for [" + request + "] but got: " + response);
        }
        return response.substring(3);
    }

    /**
     * Sends a request and asserts the response starts with "ERROR|".
     *
     * @return everything after "ERROR|"
     */
    public String sendError(String request) throws IOException {
        String response = send(request);
        if (!response.startsWith("ERROR|")) {
            throw new AssertionError("Expected ERROR for [" + request + "] but got: " + response);
        }
        return response.substring(6);
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
