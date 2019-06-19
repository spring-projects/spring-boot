package org.springframework.boot.autoconfigure.web.reactive;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.web.embedded.netty.TcpNettyServerCustomizer;
import reactor.netty.tcp.TcpServer;

public class ReactiveTcpServerCustomizer implements TcpNettyServerCustomizer {

	private final ServerProperties serverProperties;

	public ReactiveTcpServerCustomizer(
			ServerProperties serverProperties) {
		this.serverProperties = serverProperties;
	}

	@Override
	public TcpServer apply(TcpServer tcpServer) {
		InetAddress address = serverProperties.getAddress();
		Integer port = serverProperties.getPort();
		if (address != null) {
			return tcpServer
					.addressSupplier(() -> new InetSocketAddress(address.getHostAddress(), port));
		}
		return tcpServer.port(port);
	}
}
