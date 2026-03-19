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

import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.source.InvalidConfigurationPropertyValueException;
import org.springframework.boot.context.properties.source.MutuallyExclusiveConfigurationPropertiesException;
import org.springframework.boot.grpc.server.autoconfigure.GrpcServerProperties.Netty.Transport;
import org.springframework.grpc.internal.GrpcUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link NettyAddress}.
 *
 * @author Phillip Webb
 */
class NettyAddressTests {

	@Test
	void whenNoTransportAndNoAddressOrPortOrDomainSocketPathBindsToDefault() {
		NettyAddress address = new NettyAddress(null, null, null, null);
		assertThat(address).hasToString(GrpcUtils.ANY_IP_ADDRESS + ":" + GrpcUtils.DEFAULT_PORT);
	}

	@Test
	void whenNoTransportAndOnlyPortBindsToAllAddressesUsingPort() {
		NettyAddress address = new NettyAddress(null, null, 1234, null);
		assertThat(address).hasToString(GrpcUtils.ANY_IP_ADDRESS + ":1234");
	}

	@Test
	void whenNoTransportAndOnlyAddressBindsToAddressUsingPort9090() throws Exception {
		InetAddress inetAddress = InetAddress.getByName("localhost");
		NettyAddress address = new NettyAddress(null, inetAddress, null, null);
		assertThat(address).hasToString("localhost:" + GrpcUtils.DEFAULT_PORT);
	}

	@Test
	void whenNoTransportAndOnlyAddressWithoutNameBindsToAddressUsingPort9090() throws Exception {
		InetAddress inetAddress = InetAddress.getByName("192.168.1.0");
		NettyAddress address = new NettyAddress(null, inetAddress, null, null);
		assertThat(address).hasToString("192.168.1.0:" + GrpcUtils.DEFAULT_PORT);
	}

	@Test
	void whenNoTransportAndOnlyDomainSocketPathBindsToDomainSocket() {
		NettyAddress address = new NettyAddress(null, null, null, "/ds");
		assertThat(address).hasToString("unix:/ds");
	}

	@Test
	void whenNoTransportAndPortAndDomainSocketPathThrowsException() {
		NettyAddress address = new NettyAddress(null, null, 1234, "/ds");
		assertThatExceptionOfType(MutuallyExclusiveConfigurationPropertiesException.class)
			.isThrownBy(() -> address.toString())
			.withMessage(
					"The configuration properties 'spring.grpc.server.port, spring.grpc.server.netty.domain-socket-path' "
							+ "are mutually exclusive and 'spring.grpc.server.port, spring.grpc.server.netty.domain-socket-path' "
							+ "have been configured together");
	}

	@Test
	void whenNoTransportAndAddressAndDomainSocketPathThrowsException() throws Exception {
		InetAddress inetAddress = InetAddress.getByName("192.168.1.0");
		NettyAddress address = new NettyAddress(null, inetAddress, null, "/ds");
		assertThatExceptionOfType(MutuallyExclusiveConfigurationPropertiesException.class)
			.isThrownBy(() -> address.toString())
			.withMessage(
					"The configuration properties 'spring.grpc.server.address, spring.grpc.server.netty.domain-socket-path' "
							+ "are mutually exclusive and 'spring.grpc.server.address, spring.grpc.server.netty.domain-socket-path' "
							+ "have been configured together");
	}

	@Test
	void whenTcpTransportBindsToTcp() throws Exception {
		InetAddress inetAddress = InetAddress.getByName("192.168.1.0");
		NettyAddress address = new NettyAddress(Transport.TCP, inetAddress, 1234, "/ds");
		assertThat(address).hasToString("192.168.1.0:1234");
	}

	@Test
	void whenDomainSocketTransportAndNoDomainPathThrowsException() {
		NettyAddress address = new NettyAddress(Transport.DOMAIN_SOCKET, null, null, "");
		assertThatExceptionOfType(InvalidConfigurationPropertyValueException.class).isThrownBy(() -> address.toString())
			.withMessage("Property spring.grpc.server.netty.domain-socket-path with value '' is invalid: "
					+ "A path is required when spring.grpc.server.netty.transport is set to 'domain-socket'");
	}

	@Test
	void whenDomainSocketTransportAndDomainPathBindsToDomainPath() throws Exception {
		InetAddress inetAddress = InetAddress.getByName("192.168.1.0");
		NettyAddress address = new NettyAddress(Transport.DOMAIN_SOCKET, inetAddress, 1234, "/ds");
		assertThat(address).hasToString("unix:/ds");
	}

}
