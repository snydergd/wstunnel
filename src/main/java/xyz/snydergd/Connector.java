package xyz.snydergd;

import java.util.concurrent.ExecutionException;

import xyz.snydergd.nodes.Node;
import xyz.snydergd.nodes.TcpClientNode;
import xyz.snydergd.nodes.TcpServerNode;
import xyz.snydergd.nodes.WsClientNode;
import xyz.snydergd.nodes.WsServerNode;

public class Connector {
    private static Node getNode(String spec) throws InterruptedException, ExecutionException {
        System.err.println("Getting node for spec: " + spec);
        String prefix = spec.split(":", 2)[0];
        String subSpec = spec.split(":", 2)[1];
        switch (prefix.toLowerCase()) {
            case "tcpclient":
                System.err.println(" - tcpclient");
                String[] parts = subSpec.split(":");
                return new TcpClientNode(parts[0], Integer.parseInt(parts[1]));
            case "tcpserver":
                System.err.println(" - tcpserver");
                return new TcpServerNode(Integer.parseInt(subSpec));
            case "wsclient":
                System.err.println(" - wsclient");
                return new WsClientNode(subSpec);
            case "wsserver":
                System.err.println(" - wsserver");
                return new WsServerNode(Integer.parseInt(subSpec));
            default:
                System.err.println("Invalid argument prefix");
                usage();
                throw new RuntimeException("Evidently it wasn't enough that I called System.exit already");
        }
    }

    private static void usage() {
        System.err.println(
                "Expecting two arguments - the nodes you'd like connected.  These can be tcpClient:<host>:<port>, tcpServer:<port>, wsClient:<wsUrl>, or wsServer:<port>");
        System.exit(1);
    }

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        if (args.length < 2) {
            usage();
        }

        Node a, b;
        a = getNode(args[0]);
        b = getNode(args[1]);
        a.connectConsumer(b);
        b.connectConsumer(a);

        synchronized (Connector.class) {
            Connector.class.wait(); // block until we're killed with Ctrl-c or otherwise
        }
    }
}
