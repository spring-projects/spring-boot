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
import java.util.function.Supplier;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.Undertow.Builder;
import io.undertow.UndertowOptions;

import org.springframework.boot.ssl.SslBundle;
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

	/**
     * Set the customizers to be applied to the Undertow builder.
     * 
     * @param customizers the customizers to be applied
     * @throws IllegalArgumentException if the customizers are null
     */
    void setBuilderCustomizers(Collection<? extends UndertowBuilderCustomizer> customizers) {
		Assert.notNull(customizers, "Customizers must not be null");
		this.builderCustomizers = new LinkedHashSet<>(customizers);
	}

	/**
     * Add customizers to the Undertow builder.
     * 
     * @param customizers the Undertow builder customizers to add (must not be null)
     * @throws IllegalArgumentException if the customizers parameter is null
     */
    void addBuilderCustomizers(UndertowBuilderCustomizer... customizers) {
		Assert.notNull(customizers, "Customizers must not be null");
		this.builderCustomizers.addAll(Arrays.asList(customizers));
	}

	/**
     * Get the collection of UndertowBuilderCustomizer objects.
     *
     * @return the collection of UndertowBuilderCustomizer objects
     */
    Collection<UndertowBuilderCustomizer> getBuilderCustomizers() {
		return this.builderCustomizers;
	}

	/**
     * Sets the buffer size for the Undertow web server.
     * 
     * @param bufferSize the buffer size to be set
     */
    void setBufferSize(Integer bufferSize) {
		this.bufferSize = bufferSize;
	}

	/**
     * Sets the number of I/O threads to be used by the Undertow web server.
     * 
     * @param ioThreads the number of I/O threads
     */
    void setIoThreads(Integer ioThreads) {
		this.ioThreads = ioThreads;
	}

	/**
     * Sets the number of worker threads for the Undertow web server.
     * 
     * @param workerThreads the number of worker threads to set
     */
    void setWorkerThreads(Integer workerThreads) {
		this.workerThreads = workerThreads;
	}

	/**
     * Sets whether to use direct buffers for the Undertow web server.
     * 
     * @param directBuffers
     *            a boolean value indicating whether to use direct buffers
     */
    void setUseDirectBuffers(Boolean directBuffers) {
		this.directBuffers = directBuffers;
	}

	/**
     * Sets the directory where the access logs will be stored.
     * 
     * @param accessLogDirectory the directory where the access logs will be stored
     */
    void setAccessLogDirectory(File accessLogDirectory) {
		this.accessLogDirectory = accessLogDirectory;
	}

	/**
     * Sets the access log pattern for the Undertow web server.
     * 
     * @param accessLogPattern the access log pattern to be set
     */
    void setAccessLogPattern(String accessLogPattern) {
		this.accessLogPattern = accessLogPattern;
	}

	/**
     * Sets the prefix for the access log file name.
     * 
     * @param accessLogPrefix the prefix to be set for the access log file name
     */
    void setAccessLogPrefix(String accessLogPrefix) {
		this.accessLogPrefix = accessLogPrefix;
	}

	/**
     * Returns the access log prefix.
     *
     * @return the access log prefix
     */
    String getAccessLogPrefix() {
		return this.accessLogPrefix;
	}

	/**
     * Sets the suffix for the access log file.
     * 
     * @param accessLogSuffix the suffix to be set for the access log file
     */
    void setAccessLogSuffix(String accessLogSuffix) {
		this.accessLogSuffix = accessLogSuffix;
	}

	/**
     * Sets whether access log is enabled for the Undertow web server.
     * 
     * @param accessLogEnabled true to enable access log, false otherwise
     */
    void setAccessLogEnabled(boolean accessLogEnabled) {
		this.accessLogEnabled = accessLogEnabled;
	}

	/**
     * Returns a boolean value indicating whether the access log is enabled.
     *
     * @return {@code true} if the access log is enabled, {@code false} otherwise
     */
    boolean isAccessLogEnabled() {
		return this.accessLogEnabled;
	}

	/**
     * Sets whether to rotate the access log files.
     * 
     * @param accessLogRotate true to enable access log rotation, false otherwise
     */
    void setAccessLogRotate(boolean accessLogRotate) {
		this.accessLogRotate = accessLogRotate;
	}

	/**
     * Sets whether to use forward headers.
     * 
     * @param useForwardHeaders true to use forward headers, false otherwise
     */
    void setUseForwardHeaders(boolean useForwardHeaders) {
		this.useForwardHeaders = useForwardHeaders;
	}

	/**
     * Returns a boolean value indicating whether forward headers are being used.
     *
     * @return {@code true} if forward headers are being used, {@code false} otherwise
     */
    boolean isUseForwardHeaders() {
		return this.useForwardHeaders;
	}

	/**
     * Creates a builder for configuring an Undertow web server based on the provided factory and SSL bundle supplier.
     * 
     * @param factory the factory used to configure the web server
     * @param sslBundleSupplier the supplier for obtaining the SSL bundle
     * @return the configured Undertow builder
     */
    Builder createBuilder(AbstractConfigurableWebServerFactory factory, Supplier<SslBundle> sslBundleSupplier) {
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
			new SslBuilderCustomizer(factory.getPort(), address, ssl.getClientAuth(), sslBundleSupplier.get())
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

	/**
     * Creates a list of HttpHandlerFactory objects based on the provided parameters.
     * 
     * @param webServerFactory The AbstractConfigurableWebServerFactory object used to configure the web server.
     * @param initialHttpHandlerFactories An array of initial HttpHandlerFactory objects to include in the list.
     * @return A list of HttpHandlerFactory objects.
     */
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

	/**
     * Creates a list of HttpHandlerFactory objects based on the provided parameters.
     * 
     * @param compression                the Compression object to enable compression
     * @param useForwardHeaders          a boolean indicating whether to use forward headers
     * @param serverHeader               the server header value to be set
     * @param shutdown                   the Shutdown enum indicating the type of shutdown
     * @param initialHttpHandlerFactories the initial HttpHandlerFactory objects to include in the list
     * @return                           a list of HttpHandlerFactory objects
     */
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
