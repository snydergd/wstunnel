# WSTunnel - Java Version

This is a quick and dirty web socket tunnel tool written in Java, similar in ways to [this haskell one](https://github.com/erebe/wstunnel) or [this node.js one](https://www.npmjs.com/package/wstunnel).

It's a work in progress.  It's currently scraped together without much care for error handling, logging, or other best practices.

# Build
```
./gradlew build
```

# Usage Examples
The tool connects two locations (I've been calling these "nodes").

Each currently gets specified as an argument on the command line, and can be specified with the following formats/types:

- `wsclient:<websocket url>` -- Will create an outbound connection to `<websocket url>`, and send/receive from here.
- `wsserver:<port>` -- Starts undertow HTTP server on specified port and will upgrade the `/socket` path to a websocket, which will communication will be connected with.
- `tcpclient:<port>` -- Binds the specified port on and accepts TCP connections on it.  The raw bytes written/read to this port will be passed on to the opposite location.
- `tcpserver:<host>:<port>' -- Establishes a raw TCP socket with the specified host and port.

Note that the websocket connections are special.  They have a little header at the front of each frame transmitted, which gets used for multiplexing.  So if you have multiple incoming tcp connections simultaneously, there can be multiple outgoing tcp connections on the other end of the websocket.


