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
import java.util.Collection;
import java.util.List;

import io.undertow.Undertow;

import org.springframework.boot.web.reactive.server.AbstractReactiveWebServerFactory;
import org.springframework.boot.web.reactive.server.ReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServer;
import org.springframework.http.server.reactive.UndertowHttpHandlerAdapter;

/**
 * {@link ReactiveWebServerFactory} that can be used to create {@link UndertowWebServer}s.
 *
 * @author Brian Clozel
 * @since 2.0.0
 */
public class UndertowReactiveWebServerFactory extends AbstractReactiveWebServerFactory
		implements ConfigurableUndertowWebServerFactory {

	private final UndertowWebServerFactoryDelegate delegate = new UndertowWebServerFactoryDelegate();

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

	/**
     * Set the customizers to be applied to the Undertow builder.
     * 
     * @param customizers the customizers to apply
     */
    @Override
	public void setBuilderCustomizers(Collection<? extends UndertowBuilderCustomizer> customizers) {
		this.delegate.setBuilderCustomizers(customizers);
	}

	/**
     * Add customizers to the Undertow builder.
     *
     * @param customizers the customizers to add
     */
    @Override
	public void addBuilderCustomizers(UndertowBuilderCustomizer... customizers) {
		this.delegate.addBuilderCustomizers(customizers);
	}

	/**
	 * Returns a mutable collection of the {@link UndertowBuilderCustomizer}s that will be
	 * applied to the Undertow {@link io.undertow.Undertow.Builder Builder}.
	 * @return the customizers that will be applied
	 */
	public Collection<UndertowBuilderCustomizer> getBuilderCustomizers() {
		return this.delegate.getBuilderCustomizers();
	}

	/**
     * Sets the buffer size for the server.
     * 
     * @param bufferSize the buffer size to be set
     */
    @Override
	public void setBufferSize(Integer bufferSize) {
		this.delegate.setBufferSize(bufferSize);
	}

	/**
     * Sets the number of I/O threads to be used by the server.
     * 
     * @param ioThreads the number of I/O threads
     */
    @Override
	public void setIoThreads(Integer ioThreads) {
		this.delegate.setIoThreads(ioThreads);
	}

	/**
     * Sets the number of worker threads to be used by the server.
     * 
     * @param workerThreads the number of worker threads
     */
    @Override
	public void setWorkerThreads(Integer workerThreads) {
		this.delegate.setWorkerThreads(workerThreads);
	}

	/**
     * Sets whether to use direct buffers for the underlying network I/O.
     * 
     * @param directBuffers a boolean value indicating whether to use direct buffers
     */
    @Override
	public void setUseDirectBuffers(Boolean directBuffers) {
		this.delegate.setUseDirectBuffers(directBuffers);
	}

	/**
     * Sets whether to use forward headers.
     * 
     * @param useForwardHeaders true to use forward headers, false otherwise
     */
    @Override
	public void setUseForwardHeaders(boolean useForwardHeaders) {
		this.delegate.setUseForwardHeaders(useForwardHeaders);
	}

	/**
     * Returns whether to use forward headers.
     * 
     * @return {@code true} if forward headers are used, {@code false} otherwise
     */
    protected final boolean isUseForwardHeaders() {
		return this.delegate.isUseForwardHeaders();
	}

	/**
     * Sets the directory where the access logs will be stored.
     * 
     * @param accessLogDirectory the directory where the access logs will be stored
     */
    @Override
	public void setAccessLogDirectory(File accessLogDirectory) {
		this.delegate.setAccessLogDirectory(accessLogDirectory);
	}

	/**
     * Sets the access log pattern for the Undertow server.
     * 
     * @param accessLogPattern the access log pattern to be set
     */
    @Override
	public void setAccessLogPattern(String accessLogPattern) {
		this.delegate.setAccessLogPattern(accessLogPattern);
	}

	/**
     * Sets the access log prefix for the Undertow server.
     * 
     * @param accessLogPrefix the access log prefix to set
     */
    @Override
	public void setAccessLogPrefix(String accessLogPrefix) {
		this.delegate.setAccessLogPrefix(accessLogPrefix);
	}

	/**
     * Sets the suffix for the access log file.
     * 
     * @param accessLogSuffix the suffix for the access log file
     */
    @Override
	public void setAccessLogSuffix(String accessLogSuffix) {
		this.delegate.setAccessLogSuffix(accessLogSuffix);
	}

	/**
     * Returns a boolean value indicating whether the access log is enabled for this UndertowReactiveWebServerFactory instance.
     *
     * @return {@code true} if the access log is enabled, {@code false} otherwise
     */
    public boolean isAccessLogEnabled() {
		return this.delegate.isAccessLogEnabled();
	}

	/**
     * Sets whether access log is enabled for the server.
     * 
     * @param accessLogEnabled true if access log is enabled, false otherwise
     */
    @Override
	public void setAccessLogEnabled(boolean accessLogEnabled) {
		this.delegate.setAccessLogEnabled(accessLogEnabled);
	}

	/**
     * Sets whether to rotate the access log.
     * 
     * @param accessLogRotate true to rotate the access log, false otherwise
     */
    @Override
	public void setAccessLogRotate(boolean accessLogRotate) {
		this.delegate.setAccessLogRotate(accessLogRotate);
	}

	/**
     * Returns a WebServer instance for the given HttpHandler.
     * 
     * @param httpHandler the HttpHandler to be used by the WebServer
     * @return a WebServer instance
     */
    @Override
	public WebServer getWebServer(org.springframework.http.server.reactive.HttpHandler httpHandler) {
		Undertow.Builder builder = this.delegate.createBuilder(this, this::getSslBundle);
		List<HttpHandlerFactory> httpHandlerFactories = this.delegate.createHttpHandlerFactories(this,
				(next) -> new UndertowHttpHandlerAdapter(httpHandler));
		return new UndertowWebServer(builder, httpHandlerFactories, getPort() >= 0);
	}

}
