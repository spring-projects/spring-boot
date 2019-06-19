package org.springframework.boot.web.embedded.netty;

import java.util.function.Function;
import reactor.netty.tcp.TcpServer;

@FunctionalInterface
public interface TcpNettyServerCustomizer extends Function<TcpServer, TcpServer> {

}
