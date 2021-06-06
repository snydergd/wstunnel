package xyz.snydergd.nodes;

import xyz.snydergd.domain.Message;
import xyz.snydergd.exception.InvalidMessageDataException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TcpClientNode extends Node {

    private String host;
    private int port;
    private Map<Short, SocketChannel> connections = new HashMap<>();
    private Set<Thread> threadsToJoin = new HashSet<>();

    public TcpClientNode(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    protected void onOpen(Message theMessage) throws IOException {
        SocketChannel channel = SocketChannel.open(new InetSocketAddress(this.host, this.port));
        connections.put(theMessage.getConnectionNumber(), channel);
        Thread thread = new Thread(() -> {
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            int read;
            try {
                try {
                    while ((read = channel.read(buffer)) >= 0) {
                        byte[] data = new byte[read];
                        buffer.rewind();
                        buffer.get(data);
                        TcpClientNode.this.supplyMessageToConsumer(new Message(theMessage.getConnectionNumber(), Message.Type.DATA, data));
                    }
                } finally {
                    TcpClientNode.this.supplyMessageToConsumer(new Message(theMessage.getConnectionNumber(), Message.Type.CLOSE, new byte[0]));
                    TcpClientNode.this.connections.remove(theMessage.getConnectionNumber());
                }
            } catch (IOException | InvalidMessageDataException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        });
        this.threadsToJoin.add(thread);
        thread.start();

    }

    private void validateConnectionNumber(Message theMessage) throws InvalidMessageDataException {
        if (!connections.containsKey(theMessage.getConnectionNumber())) {
            throw new InvalidMessageDataException("Invalid connection number: " + theMessage.getConnectionNumber());
        }
    }
    @Override
    protected void onClose(Message theMessage) throws InvalidMessageDataException, IOException {
        validateConnectionNumber(theMessage);
        this.connections.get(theMessage.getConnectionNumber()).close();
        this.connections.remove(theMessage.getConnectionNumber());
    }

    @Override
    protected void onData(Message theMessage) throws IOException, InvalidMessageDataException {
        validateConnectionNumber(theMessage);
        this.connections.get(theMessage.getConnectionNumber()).write(ByteBuffer.wrap(theMessage.getData()));
    }
}
