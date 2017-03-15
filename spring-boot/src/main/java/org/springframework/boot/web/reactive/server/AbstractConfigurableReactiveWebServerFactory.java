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

package org.springframework.boot.web.reactive.server;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.boot.web.server.Compression;
import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.SslStoreProvider;
import org.springframework.util.Assert;

/**
 * Abstract base class for {@link ConfigurableReactiveWebServerFactory} implementations.
 *
 * @author Brian Clozel
 * @since 2.0.0
 */
public class AbstractConfigurableReactiveWebServerFactory
		implements ConfigurableReactiveWebServerFactory {

	private int port = 8080;

	private Set<ErrorPage> errorPages = new LinkedHashSet<>();

	private InetAddress address;

	private Ssl ssl;

	private SslStoreProvider sslStoreProvider;

	private Compression compression;

	private String serverHeader;

	/**
	 * Create a new {@link AbstractConfigurableReactiveWebServerFactory} instance.
	 */
	public AbstractConfigurableReactiveWebServerFactory() {
	}

	/**
	 * Create a new {@link AbstractConfigurableReactiveWebServerFactory} instance with the
	 * specified port.
	 * @param port the port number for the reactive web server
	 */
	public AbstractConfigurableReactiveWebServerFactory(int port) {
		this.port = port;
	}

	@Override
	public void setAddress(InetAddress address) {
		this.address = address;
	}

	/**
	 * Return the address that the reactive web server binds to.
	 * @return the address
	 */
	public InetAddress getAddress() {
		return this.address;
	}

	@Override
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * The port that the reactive web server listens on.
	 * @return the port
	 */
	public int getPort() {
		return this.port;
	}

	@Override
	public void setErrorPages(Set<? extends ErrorPage> errorPages) {
		Assert.notNull(errorPages, "ErrorPages must not be null");
		this.errorPages = new LinkedHashSet<>(errorPages);
	}

	@Override
	public void addErrorPages(ErrorPage... errorPages) {
		Assert.notNull(errorPages, "ErrorPages must not be null");
		this.errorPages.addAll(Arrays.asList(errorPages));
	}

	/**
	 * Return a mutable set of {@link ErrorPage ErrorPages} that will be used when
	 * handling exceptions.
	 * @return the error pages
	 */
	public Set<ErrorPage> getErrorPages() {
		return this.errorPages;
	}

	@Override
	public void setSsl(Ssl ssl) {
		this.ssl = ssl;
	}

	public Ssl getSsl() {
		return this.ssl;
	}

	@Override
	public void setSslStoreProvider(SslStoreProvider sslStoreProvider) {
		this.sslStoreProvider = sslStoreProvider;
	}

	public SslStoreProvider getSslStoreProvider() {
		return this.sslStoreProvider;
	}

	public Compression getCompression() {
		return this.compression;
	}

	@Override
	public void setCompression(Compression compression) {
		this.compression = compression;
	}

	public String getServerHeader() {
		return this.serverHeader;
	}

	@Override
	public void setServerHeader(String serverHeader) {
		this.serverHeader = serverHeader;
	}

}
