package xyz.snydergd.nodes;

import static io.undertow.Handlers.path;
import static io.undertow.Handlers.resource;
import static io.undertow.Handlers.websocket;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.undertow.Undertow;
import io.undertow.connector.ByteBufferPool;
import io.undertow.connector.PooledByteBuffer;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedBinaryMessage;
import io.undertow.websockets.core.StreamSourceFrameChannel;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import xyz.snydergd.Connector;
import xyz.snydergd.domain.Message;
import xyz.snydergd.exception.InvalidMessageDataException;

public class WsServerNode extends Node {
    private WebSocketChannel ws;
    private CountDownLatch pendingChannels = new CountDownLatch(1);
    private Thread thread;

    public WsServerNode(int port) {
        Undertow server = Undertow.builder().addHttpListener(port, "0.0.0.0")
                .setHandler(path().addPrefixPath("/socket", websocket(new WebSocketConnectionCallback() {
                    @Override
                    public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel channel) {
                        WsServerNode.this.ws = channel;
                        WsServerNode.this.pendingChannels.countDown();
                        channel.getReceiveSetter().set(new AbstractReceiveListener(){
                            private CountDownLatch ready = new CountDownLatch(1);
                            @Override
                            protected void onBinary(WebSocketChannel webSocketChannel,
                                    StreamSourceFrameChannel messageChannel) throws IOException {
                                try {
                                    ready.await();
                                    ByteBuffer buffer = ByteBuffer.allocate(1024);
                                    List<byte[]> datas = new ArrayList<>();
                                    int read;
                                    int totalLength = 0;
                                    while ((read = messageChannel.read(buffer)) >= 0) {
                                        if (read == 0) {
                                            messageChannel.awaitReadable();
                                        }
                                        totalLength += read;
                                        byte[] arr = new byte[read];
                                        buffer.flip();
                                        buffer.get(arr);
                                        datas.add(arr);
                                        buffer.rewind();
                                    }
                                    byte[] data = new byte[totalLength];
                                    int i = 0;
                                    for (byte[] arr : datas) {
                                        for (int j = 0; j < arr.length; j++) data[i+j] = arr[j];
                                        i += arr.length;
                                    }
                                    // System.err.println("Got DATA!!!     " + Stream.iterate(0, x -> x+1).limit(totalLength).map(x -> String.format("%02x", data[x])).collect(Collectors.joining(" ")));
                                    WsServerNode.this.supplyMessageToConsumer(Message.fromByteBuffer(ByteBuffer.wrap(data)));
                                } catch (InvalidMessageDataException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                } catch (InterruptedException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                                super.onBinary(webSocketChannel, messageChannel);
                            }
                            @Override
                            protected void onPing(WebSocketChannel webSocketChannel, StreamSourceFrameChannel channel)
                                    throws IOException {
                                ready.countDown();
                                super.onPing(webSocketChannel, channel);
                            }
                        });
                        channel.resumeReceives();
                    }
                })).addPrefixPath("/",
                        resource(new ClassPathResourceManager(Connector.class.getClassLoader(), Connector.class.getPackage()))
                                .addWelcomeFiles("index.html")))
                .build();
        server.start();
    }
    @Override
    public void consumeMessage(Message theMessage) throws IOException, InvalidMessageDataException {
        try {
            pendingChannels.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        synchronized (WsServerNode.class) {
            WebSockets.sendBinaryBlocking(theMessage.toByteBuffer(), this.ws);
        }
        super.consumeMessage(theMessage);
    }
    @Override
    public void close() {
        if (this.thread != null) this.thread.interrupt();
        super.close();
    }
}
