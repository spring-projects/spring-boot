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

package org.springframework.boot.web.embedded.undertow;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.accesslog.AccessLogHandler;
import io.undertow.server.handlers.accesslog.DefaultAccessLogReceiver;
import io.undertow.servlet.api.DeploymentInfo;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Xnio;
import org.xnio.XnioWorker;

import org.springframework.boot.web.reactive.server.AbstractReactiveWebServerFactory;
import org.springframework.boot.web.reactive.server.ReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServer;
import org.springframework.http.server.reactive.UndertowHttpHandlerAdapter;
import org.springframework.util.Assert;

/**
 * {@link ReactiveWebServerFactory} that can be used to create {@link UndertowWebServer}s.
 *
 * @author Brian Clozel
 * @since 2.0.0
 */
public class UndertowReactiveWebServerFactory extends AbstractReactiveWebServerFactory
		implements ConfigurableUndertowWebServerFactory {

	private List<UndertowBuilderCustomizer> builderCustomizers = new ArrayList<>();

	private List<UndertowDeploymentInfoCustomizer> deploymentInfoCustomizers = new ArrayList<>();

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
	 * Create a new {@link UndertowReactiveWebServerFactory} instance.
	 */
	public UndertowReactiveWebServerFactory() {
	}

	/**
	 * Create a new {@link UndertowReactiveWebServerFactory} that listens for requests
	 * using the specified port.
	 * @param port the port to listen on
	 */
	public UndertowReactiveWebServerFactory(int port) {
		super(port);
	}

	@Override
	public WebServer getWebServer(org.springframework.http.server.reactive.HttpHandler httpHandler) {
		Undertow.Builder builder = createBuilder(getPort());
		Closeable closeable = configureHandler(builder, httpHandler);
		return new UndertowWebServer(builder, getPort() >= 0, closeable);
	}

	private Undertow.Builder createBuilder(int port) {
		Undertow.Builder builder = Undertow.builder();
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
		if (getSsl() != null && getSsl().isEnabled()) {
			customizeSsl(builder);
		}
		else {
			builder.addHttpListener(port, getListenAddress());
		}
		for (UndertowBuilderCustomizer customizer : this.builderCustomizers) {
			customizer.customize(builder);
		}
		return builder;
	}

	private Closeable configureHandler(Undertow.Builder builder,
			org.springframework.http.server.reactive.HttpHandler httpHandler) {
		HttpHandler handler = new UndertowHttpHandlerAdapter(httpHandler);
		if (this.useForwardHeaders) {
			handler = Handlers.proxyPeerAddress(handler);
		}
		handler = UndertowCompressionConfigurer.configureCompression(getCompression(), handler);
		Closeable closeable = null;
		if (isAccessLogEnabled()) {
			closeable = configureAccessLogHandler(builder, handler);
		}
		else {
			builder.setHandler(handler);
		}
		return closeable;
	}

	private Closeable configureAccessLogHandler(Undertow.Builder builder, HttpHandler handler) {
		try {
			createAccessLogDirectoryIfNecessary();
			XnioWorker worker = createWorker();
			String prefix = (this.accessLogPrefix != null) ? this.accessLogPrefix : "access_log.";
			DefaultAccessLogReceiver accessLogReceiver = new DefaultAccessLogReceiver(worker, this.accessLogDirectory,
					prefix, this.accessLogSuffix, this.accessLogRotate);
			String formatString = ((this.accessLogPattern != null) ? this.accessLogPattern : "common");
			builder.setHandler(
					new AccessLogHandler(handler, accessLogReceiver, formatString, Undertow.class.getClassLoader()));
			return () -> {
				try {
					accessLogReceiver.close();
					worker.shutdown();
					worker.awaitTermination(30, TimeUnit.SECONDS);
				}
				catch (IOException ex) {
					throw new IllegalStateException(ex);
				}
				catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
				}
			};
		}
		catch (IOException ex) {
			throw new IllegalStateException("Failed to create AccessLogHandler", ex);
		}
	}

	private void createAccessLogDirectoryIfNecessary() {
		Assert.state(this.accessLogDirectory != null, "Access log directory is not set");
		if (!this.accessLogDirectory.isDirectory() && !this.accessLogDirectory.mkdirs()) {
			throw new IllegalStateException("Failed to create access log directory '" + this.accessLogDirectory + "'");
		}
	}

	private XnioWorker createWorker() throws IOException {
		Xnio xnio = Xnio.getInstance(Undertow.class.getClassLoader());
		return xnio.createWorker(OptionMap.builder().set(Options.THREAD_DAEMON, true).getMap());
	}

	private void customizeSsl(Undertow.Builder builder) {
		new SslBuilderCustomizer(getPort(), getAddress(), getSsl(), getSslStoreProvider()).customize(builder);
		if (getHttp2() != null) {
			builder.setServerOption(UndertowOptions.ENABLE_HTTP2, getHttp2().isEnabled());
		}
	}

	private String getListenAddress() {
		if (getAddress() == null) {
			return "0.0.0.0";
		}
		return getAddress().getHostAddress();
	}

	/**
	 * Set {@link UndertowDeploymentInfoCustomizer}s that should be applied to the
	 * Undertow {@link DeploymentInfo}. Calling this method will replace any existing
	 * customizers.
	 * @param customizers the customizers to set
	 */
	public void setDeploymentInfoCustomizers(Collection<? extends UndertowDeploymentInfoCustomizer> customizers) {
		Assert.notNull(customizers, "Customizers must not be null");
		this.deploymentInfoCustomizers = new ArrayList<>(customizers);
	}

	/**
	 * Returns a mutable collection of the {@link UndertowDeploymentInfoCustomizer}s that
	 * will be applied to the Undertow {@link DeploymentInfo}.
	 * @return the customizers that will be applied
	 */
	public Collection<UndertowDeploymentInfoCustomizer> getDeploymentInfoCustomizers() {
		return this.deploymentInfoCustomizers;
	}

	@Override
	public void addDeploymentInfoCustomizers(UndertowDeploymentInfoCustomizer... customizers) {
		Assert.notNull(customizers, "UndertowDeploymentInfoCustomizers must not be null");
		this.deploymentInfoCustomizers.addAll(Arrays.asList(customizers));
	}

	@Override
	public void setAccessLogDirectory(File accessLogDirectory) {
		this.accessLogDirectory = accessLogDirectory;
	}

	@Override
	public void setAccessLogPattern(String accessLogPattern) {
		this.accessLogPattern = accessLogPattern;
	}

	@Override
	public void setAccessLogPrefix(String accessLogPrefix) {
		this.accessLogPrefix = accessLogPrefix;
	}

	@Override
	public void setAccessLogSuffix(String accessLogSuffix) {
		this.accessLogSuffix = accessLogSuffix;
	}

	public boolean isAccessLogEnabled() {
		return this.accessLogEnabled;
	}

	@Override
	public void setAccessLogEnabled(boolean accessLogEnabled) {
		this.accessLogEnabled = accessLogEnabled;
	}

	@Override
	public void setAccessLogRotate(boolean accessLogRotate) {
		this.accessLogRotate = accessLogRotate;
	}

	protected final boolean isUseForwardHeaders() {
		return this.useForwardHeaders;
	}

	@Override
	public void setUseForwardHeaders(boolean useForwardHeaders) {
		this.useForwardHeaders = useForwardHeaders;
	}

	@Override
	public void setBufferSize(Integer bufferSize) {
		this.bufferSize = bufferSize;
	}

	@Override
	public void setIoThreads(Integer ioThreads) {
		this.ioThreads = ioThreads;
	}

	@Override
	public void setWorkerThreads(Integer workerThreads) {
		this.workerThreads = workerThreads;
	}

	@Override
	public void setUseDirectBuffers(Boolean directBuffers) {
		this.directBuffers = directBuffers;
	}

	/**
	 * Set {@link UndertowBuilderCustomizer}s that should be applied to the Undertow
	 * {@link io.undertow.Undertow.Builder Builder}. Calling this method will replace any
	 * existing customizers.
	 * @param customizers the customizers to set
	 */
	public void setBuilderCustomizers(Collection<? extends UndertowBuilderCustomizer> customizers) {
		Assert.notNull(customizers, "Customizers must not be null");
		this.builderCustomizers = new ArrayList<>(customizers);
	}

	/**
	 * Returns a mutable collection of the {@link UndertowBuilderCustomizer}s that will be
	 * applied to the Undertow {@link io.undertow.Undertow.Builder Builder}.
	 * @return the customizers that will be applied
	 */
	public Collection<UndertowBuilderCustomizer> getBuilderCustomizers() {
		return this.builderCustomizers;
	}

	/**
	 * Add {@link UndertowBuilderCustomizer}s that should be used to customize the
	 * Undertow {@link io.undertow.Undertow.Builder Builder}.
	 * @param customizers the customizers to add
	 */
	@Override
	public void addBuilderCustomizers(UndertowBuilderCustomizer... customizers) {
		Assert.notNull(customizers, "Customizers must not be null");
		this.builderCustomizers.addAll(Arrays.asList(customizers));
	}

}
