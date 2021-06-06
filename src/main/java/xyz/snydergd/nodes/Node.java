package xyz.snydergd.nodes;

import java.io.IOException;

import xyz.snydergd.domain.Message;
import xyz.snydergd.exception.InvalidMessageDataException;

public abstract class Node {
    private Node consumer;

    public void connectConsumer(Node otherNode) {
        this.consumer = otherNode;
    }

    protected void supplyMessageToConsumer(Message theMessage) throws IOException, InvalidMessageDataException {
        if (this.consumer != null) {
            this.consumer.consumeMessage(theMessage);
        }
    }

    public void consumeMessage(Message theMessage) throws IOException, InvalidMessageDataException {
        switch (theMessage.getMessageType()) {
            case OPEN:
                this.onOpen(theMessage);
                break;
            case CLOSE:
                this.onClose(theMessage);
                break;
            case DATA:
                this.onData(theMessage);
                break;
            default:
                throw new RuntimeException(new InvalidMessageDataException("This class does not yet consider the case of a message of type: "
                        + theMessage.getMessageType()));
        }
    }

    protected void onOpen(Message theMessage) throws IOException {
        /* Not implemented by default */}

    protected void onClose(Message theMessage) throws InvalidMessageDataException, IOException {
        /* Not implemented by default */}

    protected void onData(Message theMessage) throws IOException, InvalidMessageDataException {
        /* Not implemented by default */}

    public void close() {
        // do nothing if not overridden
    }
}
