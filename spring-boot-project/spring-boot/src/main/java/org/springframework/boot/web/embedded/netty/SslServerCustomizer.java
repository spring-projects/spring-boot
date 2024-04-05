/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.web.embedded.netty;

import java.util.HashMap;
import java.util.Map;

import io.netty.handler.ssl.ClientAuth;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.netty.http.Http11SslContextSpec;
import reactor.netty.http.Http2SslContextSpec;
import reactor.netty.http.server.HttpServer;
import reactor.netty.tcp.AbstractProtocolSslContextSpec;
import reactor.netty.tcp.SslProvider;
import reactor.netty.tcp.SslProvider.SslContextSpec;

import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslOptions;
import org.springframework.boot.web.server.Http2;
import org.springframework.boot.web.server.Ssl;

/**
 * {@link NettyServerCustomizer} that configures SSL for the given Reactor Netty server
 * instance.
 *
 * @author Brian Clozel
 * @author Raheela Aslam
 * @author Chris Bono
 * @author Cyril Dangerville
 * @author Scott Frederick
 * @author Moritz Halbritter
 * @author Phillip Webb
 * @since 2.0.0
 */
public class SslServerCustomizer implements NettyServerCustomizer {

	private static final Log logger = LogFactory.getLog(SslServerCustomizer.class);

	private final Http2 http2;

	private final ClientAuth clientAuth;

	private volatile SslProvider sslProvider;

	private final Map<String, SslProvider> serverNameSslProviders;

	private volatile SslBundle sslBundle;

	public SslServerCustomizer(Http2 http2, Ssl.ClientAuth clientAuth, SslBundle sslBundle,
			Map<String, SslBundle> serverNameSslBundles) {
		this.http2 = http2;
		this.clientAuth = Ssl.ClientAuth.map(clientAuth, ClientAuth.NONE, ClientAuth.OPTIONAL, ClientAuth.REQUIRE);
		this.sslBundle = sslBundle;
		this.sslProvider = createSslProvider(sslBundle);
		this.serverNameSslProviders = createServerNameSslProviders(serverNameSslBundles);
	}

	@Override
	public HttpServer apply(HttpServer server) {
		return server.secure(this::applySecurity);
	}

	private void applySecurity(SslContextSpec spec) {
		spec.sslContext(this.sslProvider.getSslContext()).setSniAsyncMappings((serverName, promise) -> {
			SslProvider provider = (serverName != null) ? this.serverNameSslProviders.get(serverName)
					: this.sslProvider;
			return promise.setSuccess(provider);
		});
	}

	void updateSslBundle(String serverName, SslBundle sslBundle) {
		logger.debug("SSL Bundle has been updated, reloading SSL configuration");
		if (serverName == null) {
			this.sslBundle = sslBundle;
			this.sslProvider = createSslProvider(sslBundle);
		}
		else {
			this.serverNameSslProviders.put(serverName, createSslProvider(sslBundle));
		}
	}

	private Map<String, SslProvider> createServerNameSslProviders(Map<String, SslBundle> serverNameSslBundles) {
		Map<String, SslProvider> serverNameSslProviders = new HashMap<>();
		serverNameSslBundles
			.forEach((serverName, sslBundle) -> serverNameSslProviders.put(serverName, createSslProvider(sslBundle)));
		return serverNameSslProviders;
	}

	private SslProvider createSslProvider(SslBundle sslBundle) {
		return SslProvider.builder().sslContext(createSslContextSpec(sslBundle)).build();
	}

	/**
	 * Factory method used to create an {@link AbstractProtocolSslContextSpec}.
	 * @return the {@link AbstractProtocolSslContextSpec} to use
	 * @deprecated since 3.2.0 for removal in 3.4.0 in favor of
	 * {@link #createSslContextSpec(SslBundle)}
	 */
	@Deprecated(since = "3.2", forRemoval = true)
	protected AbstractProtocolSslContextSpec<?> createSslContextSpec() {
		return createSslContextSpec(this.sslBundle);
	}

	/**
	 * Create an {@link AbstractProtocolSslContextSpec} for a given {@link SslBundle}.
	 * @param sslBundle the {@link SslBundle} to use
	 * @return an {@link AbstractProtocolSslContextSpec} instance
	 * @since 3.2.0
	 */
	protected final AbstractProtocolSslContextSpec<?> createSslContextSpec(SslBundle sslBundle) {
		AbstractProtocolSslContextSpec<?> sslContextSpec = (this.http2 != null && this.http2.isEnabled())
				? Http2SslContextSpec.forServer(sslBundle.getManagers().getKeyManagerFactory())
				: Http11SslContextSpec.forServer(sslBundle.getManagers().getKeyManagerFactory());
		return sslContextSpec.configure((builder) -> {
			builder.trustManager(sslBundle.getManagers().getTrustManagerFactory());
			SslOptions options = sslBundle.getOptions();
			builder.protocols(options.getEnabledProtocols());
			builder.ciphers(SslOptions.asSet(options.getCiphers()));
			builder.clientAuth(this.clientAuth);
		});
	}

}
