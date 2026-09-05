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

package org.springframework.boot.ldap.autoconfigure;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.ssl.SslBundle;
import org.springframework.util.Assert;

/**
 * Bridges the JNDI {@code java.naming.ldap.factory.socket} environment property to an
 * {@link SslBundle}. That property only accepts a class name, which JNDI loads and on
 * which it then calls {@link #getDefault()}, with no way of passing an actual factory
 * instance. The bundle to use is therefore held in a static field.
 * <p>
 * JNDI calls {@link #getDefault()} once per new connection and the {@link SslBundle} is
 * asked for a fresh {@code SSLContext} each time, so key and trust material of a bundle
 * that has been reloaded is picked up by connections opened from then on. Reloading is
 * only tracked for a bundle configured by name through {@code spring.ldap.ssl.bundle}; a
 * bundle supplied by a custom {@link LdapConnectionDetails} is used as given.
 * <p>
 * As the bundle is held statically, a single {@link SslBundle} applies JVM-wide. This is
 * sufficient for the auto-configured
 * {@link org.springframework.ldap.core.support.LdapContextSource}, of which there is at
 * most one per application context.
 * <p>
 * This class is referenced by name from a JNDI environment and is not intended to be used
 * directly.
 *
 * @author Moritz Halbritter
 * @since 4.2.0
 */
public final class LdapSslSocketFactory extends SSLSocketFactory {

	private static final Log logger = LogFactory.getLog(LdapSslSocketFactory.class);

	private static volatile @Nullable SslBundle sslBundle;

	private final SSLSocketFactory delegate;

	private LdapSslSocketFactory(SslBundle sslBundle) {
		this.delegate = sslBundle.createSslContext().getSocketFactory();
	}

	public static SocketFactory getDefault() {
		SslBundle sslBundle = LdapSslSocketFactory.sslBundle;
		Assert.state(sslBundle != null, "No SSL bundle has been set");
		return new LdapSslSocketFactory(sslBundle);
	}

	static void setSslBundle(@Nullable SslBundle sslBundle) {
		SslBundle previous = LdapSslSocketFactory.sslBundle;
		if (previous != null && sslBundle != null && previous != sslBundle) {
			logger.warn("A different SSL bundle has already been set for LDAP. As the bundle applies JVM-wide, "
					+ "LDAPS connections opened from now on use the key and trust material of the new bundle, "
					+ "including connections from context sources that were configured with the previous one");
		}
		LdapSslSocketFactory.sslBundle = sslBundle;
	}

	/**
	 * Replaces the bundle with a reloaded version of itself. Unlike
	 * {@link #setSslBundle(SslBundle)} this does not warn, as the material is being
	 * reloaded for the same bundle rather than claimed by another context source.
	 * @param sslBundle the reloaded SSL bundle
	 */
	static void updateSslBundle(SslBundle sslBundle) {
		LdapSslSocketFactory.sslBundle = sslBundle;
	}

	@Override
	public String[] getDefaultCipherSuites() {
		return this.delegate.getDefaultCipherSuites();
	}

	@Override
	public String[] getSupportedCipherSuites() {
		return this.delegate.getSupportedCipherSuites();
	}

	@Override
	public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException {
		return this.delegate.createSocket(socket, host, port, autoClose);
	}

	@Override
	public Socket createSocket(Socket socket, InputStream consumed, boolean autoClose) throws IOException {
		return this.delegate.createSocket(socket, consumed, autoClose);
	}

	/**
	 * Creates an unconnected socket. JNDI tries this first when a connect timeout has
	 * been configured, so it has to be delegated rather than inheriting
	 * {@link javax.net.SocketFactory#createSocket()}, which throws and makes JNDI fall
	 * back to a connected socket that ignores the timeout.
	 * @return an unconnected socket
	 * @throws IOException if the socket cannot be created
	 */
	@Override
	public Socket createSocket() throws IOException {
		return this.delegate.createSocket();
	}

	@Override
	public Socket createSocket(String host, int port) throws IOException {
		return this.delegate.createSocket(host, port);
	}

	@Override
	public Socket createSocket(String host, int port, InetAddress localAddress, int localPort) throws IOException {
		return this.delegate.createSocket(host, port, localAddress, localPort);
	}

	@Override
	public Socket createSocket(InetAddress host, int port) throws IOException {
		return this.delegate.createSocket(host, port);
	}

	@Override
	public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort)
			throws IOException {
		return this.delegate.createSocket(address, port, localAddress, localPort);
	}

}
