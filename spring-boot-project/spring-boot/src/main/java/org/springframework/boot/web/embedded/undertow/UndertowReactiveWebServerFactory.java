/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.web.embedded.undertow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import io.undertow.Undertow;
import io.undertow.UndertowOptions;

import org.springframework.boot.web.reactive.server.AbstractReactiveWebServerFactory;
import org.springframework.boot.web.reactive.server.ReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServer;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.UndertowHttpHandlerAdapter;
import org.springframework.util.Assert;

/**
 * {@link ReactiveWebServerFactory} that can be used to create {@link UndertowWebServer}s.
 *
 * @author Brian Clozel
 * @since 2.0.0
 */
public class UndertowReactiveWebServerFactory extends AbstractReactiveWebServerFactory {

	private Integer bufferSize;

	private Integer ioThreads;

	private Integer workerThreads;

	private Boolean directBuffers;

	private List<UndertowBuilderCustomizer> builderCustomizers = new ArrayList<>();

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
	public WebServer getWebServer(HttpHandler httpHandler) {
		Undertow.Builder builder = createBuilder(getPort());
		UndertowHttpHandlerAdapter handler = new UndertowHttpHandlerAdapter(httpHandler);
		builder.setHandler(handler);
		return new UndertowWebServer(builder, getPort() >= 0);
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

	private void customizeSsl(Undertow.Builder builder) {
		new SslBuilderCustomizer(getPort(), getAddress(), getSsl(), getSslStoreProvider())
				.customize(builder);
		if (getHttp2() != null) {
			builder.setServerOption(UndertowOptions.ENABLE_HTTP2,
					getHttp2().getEnabled());
		}
	}

	private String getListenAddress() {
		if (getAddress() == null) {
			return "0.0.0.0";
		}
		return getAddress().getHostAddress();
	}

	public void setBufferSize(Integer bufferSize) {
		this.bufferSize = bufferSize;
	}

	public void setIoThreads(Integer ioThreads) {
		this.ioThreads = ioThreads;
	}

	public void setWorkerThreads(Integer workerThreads) {
		this.workerThreads = workerThreads;
	}

	public void setDirectBuffers(Boolean directBuffers) {
		this.directBuffers = directBuffers;
	}

	/**
	 * Set {@link UndertowBuilderCustomizer}s that should be applied to the Undertow
	 * {@link io.undertow.Undertow.Builder Builder}. Calling this method will replace any
	 * existing customizers.
	 * @param customizers the customizers to set
	 */
	public void setBuilderCustomizers(
			Collection<? extends UndertowBuilderCustomizer> customizers) {
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
	public void addBuilderCustomizers(UndertowBuilderCustomizer... customizers) {
		Assert.notNull(customizers, "Customizers must not be null");
		this.builderCustomizers.addAll(Arrays.asList(customizers));
	}

}
