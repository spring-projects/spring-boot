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

package org.springframework.boot.undertow;

import java.net.InetAddress;
import java.util.Map;

import javax.net.ssl.SSLContext;

import io.undertow.Undertow;
import io.undertow.protocols.ssl.SNIContextMatcher;
import io.undertow.protocols.ssl.SNISSLContext;
import org.jspecify.annotations.Nullable;
import org.xnio.Options;
import org.xnio.Sequence;
import org.xnio.SslClientAuthMode;

import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslOptions;
import org.springframework.boot.web.server.Ssl.ClientAuth;

/**
 * {@link UndertowBuilderCustomizer} that configures SSL on the given builder instance.
 *
 * @author Brian Clozel
 * @author Raheela Aslam
 * @author Cyril Dangerville
 * @author Scott Frederick
 */
class SslBuilderCustomizer implements UndertowBuilderCustomizer {

	private final int port;

	private final @Nullable InetAddress address;

	private final @Nullable ClientAuth clientAuth;

	private final SslBundle sslBundle;

	private final Map<String, SslBundle> serverNameSslBundles;

	SslBuilderCustomizer(int port, @Nullable InetAddress address, @Nullable ClientAuth clientAuth, SslBundle sslBundle,
			Map<String, SslBundle> serverNameSslBundles) {
		this.port = port;
		this.address = address;
		this.clientAuth = clientAuth;
		this.sslBundle = sslBundle;
		this.serverNameSslBundles = serverNameSslBundles;
	}

	@Override
	public void customize(Undertow.Builder builder) {
		SslOptions options = this.sslBundle.getOptions();
		builder.addHttpsListener(this.port, getListenAddress(), createSslContext());
		builder.setSocketOption(Options.SSL_CLIENT_AUTH_MODE, ClientAuth.map(this.clientAuth,
				SslClientAuthMode.NOT_REQUESTED, SslClientAuthMode.REQUESTED, SslClientAuthMode.REQUIRED));
		if (options.getEnabledProtocols() != null) {
			builder.setSocketOption(Options.SSL_ENABLED_PROTOCOLS, Sequence.of(options.getEnabledProtocols()));
		}
		if (options.getCiphers() != null) {
			builder.setSocketOption(Options.SSL_ENABLED_CIPHER_SUITES, Sequence.of(options.getCiphers()));
		}
	}

	private SSLContext createSslContext() {
		SNIContextMatcher.Builder builder = new SNIContextMatcher.Builder();
		builder.setDefaultContext(this.sslBundle.createSslContext());
		this.serverNameSslBundles
			.forEach((serverName, sslBundle) -> builder.addMatch(serverName, sslBundle.createSslContext()));
		return new SNISSLContext(builder.build());
	}

	private String getListenAddress() {
		if (this.address == null) {
			return "0.0.0.0";
		}
		return this.address.getHostAddress();
	}

}
