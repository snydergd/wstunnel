package xyz.snydergd;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpResponse.BodySubscriber;
import java.net.http.HttpResponse.ResponseInfo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import xyz.snydergd.domain.Message;
import xyz.snydergd.exception.InvalidMessageDataException;
import xyz.snydergd.nodes.TcpClientNode;
import xyz.snydergd.nodes.TcpServerNode;
import xyz.snydergd.nodes.WsClientNode;
import xyz.snydergd.nodes.WsServerNode;

public class AppTest {

    @DisplayName("Message should serialize both forwards and backwards to byte buffer")
    @Test
    public void messageShouldSerializeBothWays() throws InvalidMessageDataException {
        Message message = new Message((short)0, Message.Type.DATA, "hi".getBytes());
        assertArrayEquals(message.toByteBuffer().array(), Message.fromByteBuffer(message.toByteBuffer()).toByteBuffer().array());
    }
    @DisplayName("Should have valid data when selialized")
    @Test
    public void messageSerializesWithData() throws InvalidMessageDataException {
        Message message = new Message((short)0, Message.Type.DATA, new byte[] { 4 });
        assertEquals(4, message.toByteBuffer().array()[5]);
    }

    @Test
    public void tcpShouldConnectToTcp() throws Exception {
        HttpClient client = HttpClient.newBuilder().build();

        TcpServerNode tcpListener = new TcpServerNode(8083);
        WsServerNode wsserver = new WsServerNode(8082);
        TcpClientNode tcpsender = new TcpClientNode("localhost", 8082);
        WsClientNode wsclient = new WsClientNode("ws://localhost:8082/socket");

        /*
            Test traffic -> tcp server 8081 -> websocket server (8080) -> WsClient -> tcpclient (hitting ws server for files)
        */
        tcpListener.connectConsumer(wsclient);
        wsclient.connectConsumer(tcpListener);

        wsserver.connectConsumer(tcpsender);
        tcpsender.connectConsumer(wsserver);

        assertEquals(200, client.sendAsync(HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:8083/"))
            .build(), BodyHandlers.ofString())
            .get()
            .statusCode());
        tcpListener.close();
        wsserver.close();
        wsclient.close();
        tcpsender.close();
    
    }
}
