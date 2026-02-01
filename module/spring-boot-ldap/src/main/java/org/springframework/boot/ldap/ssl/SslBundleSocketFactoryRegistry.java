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

package org.springframework.boot.ldap.ssl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

/**
 * Registry-based {@link SSLSocketFactory} for LDAPS connections.
 * <p>
 * JNDI requires the {@code java.naming.ldap.factory.socket} property to be a class name,
 * not an instance. This class provides a workaround by storing {@link SSLSocketFactory}
 * instances in a {@link ThreadLocal} before context creation.
 * <p>
 * Usage:
 * <ol>
 * <li>Register the SSLSocketFactory:
 * {@code SslBundleSocketFactoryRegistry.register(sslSocketFactory)}</li>
 * <li>Set JNDI property:
 * {@code env.put("java.naming.ldap.factory.socket", SslBundleSocketFactoryRegistry.class.getName())}</li>
 * <li>Create LDAP context (uses the registered factory)</li>
 * <li>Clear registration: {@code SslBundleSocketFactoryRegistry.clear()}</li>
 * </ol>
 *
 * @author Massimo Deiana
 * @since 4.0.0
 * @see SSLSocketFactory
 */
public class SslBundleSocketFactoryRegistry extends SSLSocketFactory {

	private static final ThreadLocal<SSLSocketFactory> CURRENT_FACTORY = new ThreadLocal<>();

	private final SSLSocketFactory delegate;

	/**
	 * Default constructor required by JNDI. Retrieves the {@link SSLSocketFactory} from
	 * the {@link ThreadLocal} registry.
	 * @throws IllegalStateException if no factory has been registered
	 */
	public SslBundleSocketFactoryRegistry() {
		SSLSocketFactory factory = CURRENT_FACTORY.get();
		if (factory == null) {
			throw new IllegalStateException(
					"No SSLSocketFactory registered. Call SslBundleSocketFactoryRegistry.register() "
							+ "before creating the LDAP context.");
		}
		this.delegate = factory;
	}

	/**
	 * Register an {@link SSLSocketFactory} for the current thread. Must be called before
	 * creating the LDAP context.
	 * @param factory the SSLSocketFactory to use for LDAPS connections
	 */
	public static void register(SSLSocketFactory factory) {
		CURRENT_FACTORY.set(factory);
	}

	/**
	 * Clear the registered {@link SSLSocketFactory} for the current thread. Should be
	 * called after LDAP context creation to prevent memory leaks.
	 */
	public static void clear() {
		CURRENT_FACTORY.remove();
	}

	/**
	 * Returns a new instance that delegates to the registered factory. This method is
	 * called by JNDI when creating SSL sockets.
	 * @return a new {@link SslBundleSocketFactoryRegistry} instance
	 */
	public static SocketFactory getDefault() {
		return new SslBundleSocketFactoryRegistry();
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
	public Socket createSocket(String host, int port) throws IOException {
		return this.delegate.createSocket(host, port);
	}

	@Override
	public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
		return this.delegate.createSocket(host, port, localHost, localPort);
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

	@Override
	public Socket createSocket() throws IOException {
		return this.delegate.createSocket();
	}

}
