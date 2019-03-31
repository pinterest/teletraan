package com.pinterest.teletraan.resource;

import java.io.DataOutputStream;
import java.net.Socket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TCPClientForMetrics {
    private static final Logger LOG = LoggerFactory.getLogger(TCPClientForMetrics.class);
    public static final int PORT = 18126;

    public void emitMetrics(String metricsName) throws Exception {
        String sentence = "";
        Socket clientSocket = null;
        DataOutputStream outToServer = null;
        try {
            clientSocket = new Socket("127.0.0.1", PORT);
            outToServer = new DataOutputStream(clientSocket.getOutputStream());
            sentence = String.format("put %s %d %s", metricsName, System.currentTimeMillis(), "1 host=localhost");
            outToServer.writeBytes(sentence + '\n');
        } catch (Exception e) {
            LOG.error("TCPClientForMetrics exception {} ", e.toString());
        } finally {
            outToServer.close();
            clientSocket.close();
        }

    }
}