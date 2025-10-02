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

package org.springframework.boot.http.client.reactive;

import java.net.SocketAddress;
import java.util.List;

import reactor.netty.transport.ClientTransport;
import reactor.netty.transport.ClientTransportConfig;

import org.springframework.boot.web.client.SecurityDnsHandler;
import org.springframework.util.Assert;

/**
 * A {@link ClientTransport.ResolvedAddressSelector} that filters resolved addresses using
 * a {@link SecurityDnsHandler}.
 *
 * @author Phillip Webb
 * @author Kian Jamali
 */
class NettyHttpClientAddressSelector implements ClientTransport.ResolvedAddressSelector<ClientTransportConfig<?>> {

	private final SecurityDnsHandler securityDnsHandler;

	NettyHttpClientAddressSelector(SecurityDnsHandler securityDnsHandler) {
		Assert.notNull(securityDnsHandler, "SecurityDnsHandler must not be null");
		this.securityDnsHandler = securityDnsHandler;
	}

	@Override
	public List<? extends SocketAddress> apply(ClientTransportConfig<?> clientTransportConfig,
			List<? extends SocketAddress> resolvedAddresses) {
		if (resolvedAddresses.isEmpty()) {
			return resolvedAddresses;
		}
		return this.securityDnsHandler.handleSocketAddresses(resolvedAddresses);
	}

}
