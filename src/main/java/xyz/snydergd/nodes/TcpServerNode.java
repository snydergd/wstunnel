package xyz.snydergd.nodes;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Logger;

import xyz.snydergd.domain.Message;
import xyz.snydergd.exception.InvalidMessageDataException;

public class TcpServerNode extends Node {
    private static final Logger logger = Logger.getLogger(TcpServerNode.class.getName());

    private Thread listenerThread;
    private Map<Short, SocketChannel> connections = new HashMap<>();
    private Queue<Short> pendingConnectionNumbers = new LinkedList<>();
    private Map<Short, List<byte[]>> pendingData = new HashMap<>();
    private List<Thread> threadsToJoin = new ArrayList<>();
    private short nextConnectionId = 0;

    public TcpServerNode(int port) {
        this.listenerThread = new Thread(() -> {
            try {
                ServerSocketChannel serverSocket = ServerSocketChannel.open();
                serverSocket.bind(new InetSocketAddress(port));
                while (true) {
                    SocketChannel socket = serverSocket.accept();
                    short connectionNumber;
                    if (!TcpServerNode.this.pendingConnectionNumbers.isEmpty()) {
                        connectionNumber = TcpServerNode.this.pendingConnectionNumbers.remove();
                        TcpServerNode.this.supplyMessageToConsumer(new Message(connectionNumber, Message.Type.OPEN, ByteBuffer.allocate(0)));
                        for (byte[] data : TcpServerNode.this.pendingData.get(connectionNumber)) {
                            socket.write(ByteBuffer.wrap(data));
                        }
                        TcpServerNode.this.pendingData.remove(connectionNumber);
                    } else {
                        connectionNumber = nextConnectionId++;
                        TcpServerNode.this.supplyMessageToConsumer(new Message(connectionNumber, Message.Type.OPEN, ByteBuffer.allocate(0)));
                    }
                    this.connections.put(connectionNumber, socket);
                    socket.configureBlocking(true);
                    Thread thread = new Thread(() -> {
                        ByteBuffer buffer = ByteBuffer.allocate(256);
                        try {
                            try {
                                while (socket.read(buffer) >= 0) {
                                    buffer.flip();
                                    TcpServerNode.this.supplyMessageToConsumer(new Message(connectionNumber, Message.Type.DATA, buffer));
                                    buffer.rewind();
                                }
                            } finally {
                                TcpServerNode.this.supplyMessageToConsumer(new Message(connectionNumber, Message.Type.CLOSE, new byte[0]));
                            }
                        } catch (IOException | InvalidMessageDataException e) {
                            e.printStackTrace();
                        }
                    });
                    threadsToJoin.add(thread);
                    thread.start();
                }
            } catch (IOException | InvalidMessageDataException e) {
                e.printStackTrace();
            }
        });
        this.listenerThread.start();
    }

    @Override
    protected void onOpen(Message theMessage) throws IOException {
        pendingConnectionNumbers.add(theMessage.getConnectionNumber());
        pendingData.put(theMessage.getConnectionNumber(), new ArrayList<>());
        if (nextConnectionId < theMessage.getConnectionNumber()) nextConnectionId = (short)(theMessage.getConnectionNumber()+1);
    }

    @Override
    protected void onClose(Message theMessage) throws InvalidMessageDataException, IOException {
        if (this.connections.containsKey(theMessage.getConnectionNumber())) {
            this.connections.get(theMessage.getConnectionNumber()).close();
            this.connections.remove(theMessage.getConnectionNumber());
        } else if (this.pendingData.containsKey(theMessage.getConnectionNumber())) {
            this.pendingConnectionNumbers.remove(theMessage.getConnectionNumber());
            this.pendingData.remove(theMessage.getConnectionNumber());
        } else {
            logger.severe("Close requested for non-existent connection: " + theMessage.getConnectionNumber());
        }
    }

    @Override
    protected void onData(Message theMessage) throws IOException, InvalidMessageDataException {
        if (this.connections.containsKey(theMessage.getConnectionNumber())) {
            this.connections.get(theMessage.getConnectionNumber()).write(ByteBuffer.wrap(theMessage.getData()));
        } else if (this.pendingData.containsKey(theMessage.getConnectionNumber())) {
            this.pendingData.get(theMessage.getConnectionNumber()).add(theMessage.getData());
        } else {
            throw new InvalidMessageDataException("No connection is currently established with number: " + theMessage.getConnectionNumber());
        }
    }
    @Override
    public void close() {
        this.listenerThread.interrupt();
        for (Thread t : this.threadsToJoin) {
            t.interrupt();
        }
    }
}
