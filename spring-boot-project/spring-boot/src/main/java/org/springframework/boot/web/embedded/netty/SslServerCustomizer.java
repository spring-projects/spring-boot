/*
 * Copyright 2012-2023 the original author or authors.
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

import io.netty.handler.ssl.ClientAuth;
import reactor.netty.http.Http11SslContextSpec;
import reactor.netty.http.Http2SslContextSpec;
import reactor.netty.http.server.HttpServer;
import reactor.netty.tcp.AbstractProtocolSslContextSpec;

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
 * @since 2.0.0
 */
public class SslServerCustomizer implements NettyServerCustomizer {

	private final Http2 http2;

	private final Ssl.ClientAuth clientAuth;

	private final SslBundle sslBundle;

	public SslServerCustomizer(Http2 http2, Ssl.ClientAuth clientAuth, SslBundle sslBundle) {
		this.http2 = http2;
		this.clientAuth = clientAuth;
		this.sslBundle = sslBundle;
	}

	@Override
	public HttpServer apply(HttpServer server) {
		AbstractProtocolSslContextSpec<?> sslContextSpec = createSslContextSpec();
		return server.secure((spec) -> spec.sslContext(sslContextSpec));
	}

	protected AbstractProtocolSslContextSpec<?> createSslContextSpec() {
		AbstractProtocolSslContextSpec<?> sslContextSpec = (this.http2 != null && this.http2.isEnabled())
				? Http2SslContextSpec.forServer(this.sslBundle.getManagers().getKeyManagerFactory())
				: Http11SslContextSpec.forServer(this.sslBundle.getManagers().getKeyManagerFactory());
		sslContextSpec.configure((builder) -> {
			builder.trustManager(this.sslBundle.getManagers().getTrustManagerFactory());
			SslOptions options = this.sslBundle.getOptions();
			builder.protocols(options.getEnabledProtocols());
			builder.ciphers(SslOptions.asSet(options.getCiphers()));
			builder.clientAuth(org.springframework.boot.web.server.Ssl.ClientAuth.map(this.clientAuth, ClientAuth.NONE,
					ClientAuth.OPTIONAL, ClientAuth.REQUIRE));
		});
		return sslContextSpec;
	}

}
