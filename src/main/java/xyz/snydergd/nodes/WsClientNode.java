package xyz.snydergd.nodes;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.net.http.WebSocket.Listener;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import xyz.snydergd.domain.Message;
import xyz.snydergd.exception.InvalidMessageDataException;

public class WsClientNode extends Node {
    private WebSocket ws;
    private CountDownLatch unreadyCount = new CountDownLatch(1);
    public WsClientNode(String url) throws InterruptedException, ExecutionException {
        WebSocket.Listener listener = new WebSocket.Listener(){
            @Override
            public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
                int accumulatedLength = 0;
                List<byte[]> dataReceived = new ArrayList<>();
                try {
                    if (dataReceived.isEmpty() && last) {
                        byte[] stuff = new byte[data.remaining()];
                        data.get(stuff);
                        data.rewind();
                        System.out.println("=====   DATA   ===== " + stuff.length + "\n" + new String(stuff));
                        WsClientNode.this.supplyMessageToConsumer(Message.fromByteBuffer(data));
                    } else {
                        byte[] arr = new byte[data.remaining()];
                        data.get(arr);
                        dataReceived.add(arr);
                        accumulatedLength += arr.length;
                        if (last) {
                            ByteBuffer allData = ByteBuffer.allocate(accumulatedLength);
                            for (byte[] item : dataReceived) {
                                allData.put(item);
                            }
                            allData.flip();
                            System.out.println("=====   DATA   ===== " + allData.remaining() + "\n" + new String(allData.array()));
                            allData.rewind();
                            WsClientNode.this.supplyMessageToConsumer(Message.fromByteBuffer(data));
                            accumulatedLength = 0;
                            dataReceived.clear();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InvalidMessageDataException e) {
                    e.printStackTrace();
                }
                return Listener.super.onBinary(webSocket, data, last);
            }
            @Override
            public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
                unreadyCount.countDown();
                return Listener.super.onPong(webSocket, message);
            }
        };
        ws = HttpClient.newHttpClient().newWebSocketBuilder().buildAsync(URI.create(url), listener).get();
        while (unreadyCount.getCount() > 0) {
            ws.sendPing(ByteBuffer.allocate(0));
            unreadyCount.await(1, TimeUnit.SECONDS);
        }
    }
    @Override
    public void consumeMessage(Message theMessage) throws IOException, InvalidMessageDataException {
        try {
            unreadyCount.await();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        this.ws.sendBinary(theMessage.toByteBuffer(), true);
        super.consumeMessage(theMessage);
    }
}
