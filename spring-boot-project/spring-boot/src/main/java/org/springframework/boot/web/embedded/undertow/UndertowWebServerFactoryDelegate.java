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

package org.springframework.boot.web.embedded.undertow;

import java.io.File;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.Undertow.Builder;
import io.undertow.UndertowOptions;

import org.springframework.boot.web.server.AbstractConfigurableWebServerFactory;
import org.springframework.boot.web.server.Compression;
import org.springframework.boot.web.server.Http2;
import org.springframework.boot.web.server.Shutdown;
import org.springframework.boot.web.server.Ssl;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Delegate class used by {@link UndertowServletWebServerFactory} and
 * {@link UndertowReactiveWebServerFactory}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class UndertowWebServerFactoryDelegate {

	private Set<UndertowBuilderCustomizer> builderCustomizers = new LinkedHashSet<>();

	private Integer bufferSize;

	private Integer ioThreads;

	private Integer workerThreads;

	private Boolean directBuffers;

	private File accessLogDirectory;

	private String accessLogPattern;

	private String accessLogPrefix;

	private String accessLogSuffix;

	private boolean accessLogEnabled = false;

	private boolean accessLogRotate = true;

	private boolean useForwardHeaders;

	void setBuilderCustomizers(Collection<? extends UndertowBuilderCustomizer> customizers) {
		Assert.notNull(customizers, "Customizers must not be null");
		this.builderCustomizers = new LinkedHashSet<>(customizers);
	}

	void addBuilderCustomizers(UndertowBuilderCustomizer... customizers) {
		Assert.notNull(customizers, "Customizers must not be null");
		this.builderCustomizers.addAll(Arrays.asList(customizers));
	}

	Collection<UndertowBuilderCustomizer> getBuilderCustomizers() {
		return this.builderCustomizers;
	}

	void setBufferSize(Integer bufferSize) {
		this.bufferSize = bufferSize;
	}

	void setIoThreads(Integer ioThreads) {
		this.ioThreads = ioThreads;
	}

	void setWorkerThreads(Integer workerThreads) {
		this.workerThreads = workerThreads;
	}

	void setUseDirectBuffers(Boolean directBuffers) {
		this.directBuffers = directBuffers;
	}

	void setAccessLogDirectory(File accessLogDirectory) {
		this.accessLogDirectory = accessLogDirectory;
	}

	void setAccessLogPattern(String accessLogPattern) {
		this.accessLogPattern = accessLogPattern;
	}

	void setAccessLogPrefix(String accessLogPrefix) {
		this.accessLogPrefix = accessLogPrefix;
	}

	String getAccessLogPrefix() {
		return this.accessLogPrefix;
	}

	void setAccessLogSuffix(String accessLogSuffix) {
		this.accessLogSuffix = accessLogSuffix;
	}

	void setAccessLogEnabled(boolean accessLogEnabled) {
		this.accessLogEnabled = accessLogEnabled;
	}

	boolean isAccessLogEnabled() {
		return this.accessLogEnabled;
	}

	void setAccessLogRotate(boolean accessLogRotate) {
		this.accessLogRotate = accessLogRotate;
	}

	void setUseForwardHeaders(boolean useForwardHeaders) {
		this.useForwardHeaders = useForwardHeaders;
	}

	boolean isUseForwardHeaders() {
		return this.useForwardHeaders;
	}

	Builder createBuilder(AbstractConfigurableWebServerFactory factory) {
		Ssl ssl = factory.getSsl();
		InetAddress address = factory.getAddress();
		int port = factory.getPort();
		Builder builder = Undertow.builder();
		if (this.bufferSize != null) {
			builder.setBufferSize(this.bufferSize);
		}
		if (this.ioThreads != null) {
			builder.setIoThreads(this.ioThreads);
		}
		if (this.workerThreads != null) {
			builder.setWorkerThreads(this.workerThreads);
		}
		if (this.directBuffers != null) {
			builder.setDirectBuffers(this.directBuffers);
		}
		Http2 http2 = factory.getHttp2();
		if (http2 != null) {
			builder.setServerOption(UndertowOptions.ENABLE_HTTP2, http2.isEnabled());
		}
		if (ssl != null && ssl.isEnabled()) {
			new SslBuilderCustomizer(factory.getPort(), address, ssl, factory.getOrCreateSslStoreProvider())
				.customize(builder);
		}
		else {
			builder.addHttpListener(port, (address != null) ? address.getHostAddress() : "0.0.0.0");
		}
		builder.setServerOption(UndertowOptions.SHUTDOWN_TIMEOUT, 0);
		for (UndertowBuilderCustomizer customizer : this.builderCustomizers) {
			customizer.customize(builder);
		}
		return builder;
	}

	List<HttpHandlerFactory> createHttpHandlerFactories(AbstractConfigurableWebServerFactory webServerFactory,
			HttpHandlerFactory... initialHttpHandlerFactories) {
		List<HttpHandlerFactory> factories = createHttpHandlerFactories(webServerFactory.getCompression(),
				this.useForwardHeaders, webServerFactory.getServerHeader(), webServerFactory.getShutdown(),
				initialHttpHandlerFactories);
		if (isAccessLogEnabled()) {
			factories.add(new AccessLogHttpHandlerFactory(this.accessLogDirectory, this.accessLogPattern,
					this.accessLogPrefix, this.accessLogSuffix, this.accessLogRotate));
		}
		return factories;
	}

	static List<HttpHandlerFactory> createHttpHandlerFactories(Compression compression, boolean useForwardHeaders,
			String serverHeader, Shutdown shutdown, HttpHandlerFactory... initialHttpHandlerFactories) {
		List<HttpHandlerFactory> factories = new ArrayList<>(Arrays.asList(initialHttpHandlerFactories));
		if (compression != null && compression.getEnabled()) {
			factories.add(new CompressionHttpHandlerFactory(compression));
		}
		if (useForwardHeaders) {
			factories.add(Handlers::proxyPeerAddress);
		}
		if (StringUtils.hasText(serverHeader)) {
			factories.add((next) -> Handlers.header(next, "Server", serverHeader));
		}
		if (shutdown == Shutdown.GRACEFUL) {
			factories.add(Handlers::gracefulShutdown);
		}
		return factories;
	}

}
