/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.web.reactive;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import reactor.netty.tcp.TcpServer;

import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.web.embedded.netty.TcpNettyServerCustomizer;

/**
 * The customizer to apply {@link ServerProperties} to Netty {@link TcpServer}.
 *
 * @author Eugene Utkin
 */
public class ReactiveTcpServerCustomizer implements TcpNettyServerCustomizer {

	private final ServerProperties serverProperties;

	public ReactiveTcpServerCustomizer(ServerProperties serverProperties) {
		this.serverProperties = serverProperties;
	}

	@Override
	public TcpServer apply(TcpServer tcpServer) {
		InetAddress address = this.serverProperties.getAddress();
		Integer port = this.serverProperties.getPort();
		if (address != null) {
			return tcpServer.addressSupplier(() -> new InetSocketAddress(address.getHostAddress(), port));
		}
		return tcpServer.port(port);
	}

}
