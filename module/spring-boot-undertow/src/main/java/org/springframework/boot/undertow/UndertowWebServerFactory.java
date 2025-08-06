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

import java.io.File;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.Undertow.Builder;
import io.undertow.UndertowOptions;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.web.server.AbstractConfigurableWebServerFactory;
import org.springframework.boot.web.server.Compression;
import org.springframework.boot.web.server.Http2;
import org.springframework.boot.web.server.Shutdown;
import org.springframework.boot.web.server.Ssl;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Base class for factories that produce an {@link UndertowWebServer}.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
public abstract class UndertowWebServerFactory extends AbstractConfigurableWebServerFactory
		implements ConfigurableUndertowWebServerFactory {

	private Set<UndertowBuilderCustomizer> builderCustomizers = new LinkedHashSet<>();

	private @Nullable Integer bufferSize;

	private @Nullable Integer ioThreads;

	private @Nullable Integer workerThreads;

	private @Nullable Boolean directBuffers;

	private @Nullable File accessLogDirectory;

	private @Nullable String accessLogPattern;

	private @Nullable String accessLogPrefix;

	private @Nullable String accessLogSuffix;

	private boolean accessLogEnabled;

	private boolean accessLogRotate = true;

	private boolean useForwardHeaders;

	protected UndertowWebServerFactory() {
	}

	protected UndertowWebServerFactory(int port) {
		super(port);
	}

	public Collection<UndertowBuilderCustomizer> getBuilderCustomizers() {
		return this.builderCustomizers;
	}

	@Override
	public void setBuilderCustomizers(Collection<? extends UndertowBuilderCustomizer> customizers) {
		Assert.notNull(customizers, "'customizers' must not be null");
		this.builderCustomizers = new LinkedHashSet<>(customizers);
	}

	@Override
	public void addBuilderCustomizers(UndertowBuilderCustomizer... customizers) {
		Assert.notNull(customizers, "'customizers' must not be null");
		this.builderCustomizers.addAll(Arrays.asList(customizers));
	}

	@Override
	public void setBufferSize(@Nullable Integer bufferSize) {
		this.bufferSize = bufferSize;
	}

	@Override
	public void setIoThreads(@Nullable Integer ioThreads) {
		this.ioThreads = ioThreads;
	}

	@Override
	public void setWorkerThreads(@Nullable Integer workerThreads) {
		this.workerThreads = workerThreads;
	}

	@Override
	public void setUseDirectBuffers(@Nullable Boolean directBuffers) {
		this.directBuffers = directBuffers;
	}

	@Override
	public void setAccessLogDirectory(@Nullable File accessLogDirectory) {
		this.accessLogDirectory = accessLogDirectory;
	}

	@Override
	public void setAccessLogPattern(@Nullable String accessLogPattern) {
		this.accessLogPattern = accessLogPattern;
	}

	@Override
	public void setAccessLogPrefix(@Nullable String accessLogPrefix) {
		this.accessLogPrefix = accessLogPrefix;
	}

	public @Nullable String getAccessLogPrefix() {
		return this.accessLogPrefix;
	}

	@Override
	public void setAccessLogSuffix(@Nullable String accessLogSuffix) {
		this.accessLogSuffix = accessLogSuffix;
	}

	@Override
	public void setAccessLogEnabled(boolean accessLogEnabled) {
		this.accessLogEnabled = accessLogEnabled;
	}

	public boolean isAccessLogEnabled() {
		return this.accessLogEnabled;
	}

	@Override
	public void setAccessLogRotate(boolean accessLogRotate) {
		this.accessLogRotate = accessLogRotate;
	}

	@Override
	public void setUseForwardHeaders(boolean useForwardHeaders) {
		this.useForwardHeaders = useForwardHeaders;
	}

	public boolean isUseForwardHeaders() {
		return this.useForwardHeaders;
	}

	public Builder createBuilder(AbstractConfigurableWebServerFactory factory, Supplier<SslBundle> sslBundleSupplier,
			Supplier<Map<String, SslBundle>> serverNameSslBundlesSupplier) {
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
		Ssl ssl = factory.getSsl();
		if (Ssl.isEnabled(ssl)) {
			new SslBuilderCustomizer(factory.getPort(), address, ssl.getClientAuth(), sslBundleSupplier.get(),
					serverNameSslBundlesSupplier.get())
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

	public List<HttpHandlerFactory> createHttpHandlerFactories(AbstractConfigurableWebServerFactory webServerFactory,
			HttpHandlerFactory... initialHttpHandlerFactories) {
		List<HttpHandlerFactory> factories = createHttpHandlerFactories(webServerFactory.getCompression(),
				this.useForwardHeaders, webServerFactory.getServerHeader(), webServerFactory.getShutdown(),
				initialHttpHandlerFactories);
		if (isAccessLogEnabled()) {
			Assert.state(this.accessLogDirectory != null, "Access log directory is not set");
			factories.add(new AccessLogHttpHandlerFactory(this.accessLogDirectory, this.accessLogPattern,
					this.accessLogPrefix, this.accessLogSuffix, this.accessLogRotate));
		}
		return factories;
	}

	static List<HttpHandlerFactory> createHttpHandlerFactories(@Nullable Compression compression,
			boolean useForwardHeaders, @Nullable String serverHeader, Shutdown shutdown,
			HttpHandlerFactory... initialHttpHandlerFactories) {
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
