Java Blue Legion WebSockets Server
===============

This repository uses the libaries from https://github.com/TooTallNate/Java-WebSocket to implement a GameServer class for my team project game, Blue Legion.

Blue Legion is an online, turn based PvP multiplayer game which involves players selecting cards per turn in order to beat the opponent. 

The underlying classes are implemented `java.nio`, which allows for a
non-blocking event-driven model (similar to the
[WebSocket API](https://html.spec.whatwg.org/multipage/web-sockets.html) for web browsers).

Implemented WebSocket protocol versions are:

 * [RFC 6455](http://tools.ietf.org/html/rfc6455)
 * [RFC 7692](http://tools.ietf.org/html/rfc7692)

Client code: https://github.com/Xakep1101/BlueLegion-Client

Minimum Required JDK
--------------------

`Java-WebSocket` is known to work with:

 * Java 1.7 and higher
