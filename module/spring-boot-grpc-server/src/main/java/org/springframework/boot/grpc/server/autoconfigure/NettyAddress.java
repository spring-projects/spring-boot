/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.grpc.server.autoconfigure;

import java.net.InetAddress;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.source.InvalidConfigurationPropertyValueException;
import org.springframework.boot.context.properties.source.MutuallyExclusiveConfigurationPropertiesException;
import org.springframework.boot.grpc.server.autoconfigure.GrpcServerProperties.Netty.Transport;
import org.springframework.grpc.internal.GrpcUtils;
import org.springframework.grpc.server.GrpcServerFactory;
import org.springframework.util.StringUtils;

/**
 * Address {@link GrpcServerFactory} address.
 *
 * @author Phillip Webb
 * @param transport the transport to use
 * @param address the bind address
 * @param port the listen port
 * @param domainSocketPath the domain socket path
 */
record NettyAddress(@Nullable Transport transport, @Nullable InetAddress address, @Nullable Integer port,
		@Nullable String domainSocketPath) {

	@Override
	public final String toString() {
		Transport transport = (this.transport != null) ? this.transport : deduceTransport();
		return switch (transport) {
			case TCP -> tcpAddress();
			case DOMAIN_SOCKET -> domainSocketAddress();
		};
	}

	private Transport deduceTransport() {
		MutuallyExclusiveConfigurationPropertiesException.throwIfMultipleNonNullValuesIn((entries) -> {
			entries.put("spring.grpc.server.address", this.address);
			entries.put("spring.grpc.server.netty.domain-socket-path", this.domainSocketPath);
		});
		MutuallyExclusiveConfigurationPropertiesException.throwIfMultipleNonNullValuesIn((entries) -> {
			entries.put("spring.grpc.server.port", this.port);
			entries.put("spring.grpc.server.netty.domain-socket-path", this.domainSocketPath);
		});
		if (this.address != null || this.port != null) {
			return Transport.TCP;
		}
		if (this.domainSocketPath != null) {
			return Transport.DOMAIN_SOCKET;
		}
		return Transport.TCP;
	}

	private String tcpAddress() {
		String address = (this.address != null) ? toString(this.address) : GrpcUtils.ANY_IP_ADDRESS;
		int port = (this.port != null) ? this.port : GrpcUtils.DEFAULT_PORT;
		return address + ":" + port;
	}

	private String domainSocketAddress() {
		if (!StringUtils.hasText(this.domainSocketPath)) {
			throw new InvalidConfigurationPropertyValueException("spring.grpc.server.netty.domain-socket-path",
					this.domainSocketPath,
					"A path is required when spring.grpc.server.netty.transport is set to 'domain-socket'");
		}
		return "unix:" + this.domainSocketPath;
	}

	private static String toString(InetAddress address) {
		String hostName = address.getHostName();
		return (hostName != null) ? hostName : address.getHostAddress();
	}

	static NettyAddress fromProperties(GrpcServerProperties properties) {
		return new NettyAddress(properties.getNetty().getTransport(), properties.getAddress(), properties.getPort(),
				properties.getNetty().getDomainSocketPath());
	}

}
