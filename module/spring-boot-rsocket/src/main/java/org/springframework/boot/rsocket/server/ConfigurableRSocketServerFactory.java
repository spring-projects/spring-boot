/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.rsocket.server;

import java.net.InetAddress;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.web.server.Ssl;
import org.springframework.util.unit.DataSize;

/**
 * A configurable {@link RSocketServerFactory}.
 *
 * @author Brian Clozel
 * @author Scott Frederick
 * @since 2.2.0
 */
public interface ConfigurableRSocketServerFactory {

	/**
	 * Set the port that the server should listen on. If not specified port '9898' will be
	 * used.
	 * @param port the port to set
	 */
	void setPort(int port);

	/**
	 * Specify the maximum transmission unit. Frames larger than the specified
	 * {@code fragmentSize} are fragmented.
	 * @param fragmentSize the fragment size
	 * @since 2.4.0
	 */
	void setFragmentSize(@Nullable DataSize fragmentSize);

	/**
	 * Set the specific network address that the server should bind to.
	 * @param address the address to set (defaults to {@code null})
	 */
	void setAddress(@Nullable InetAddress address);

	/**
	 * Set the transport that the RSocket server should use.
	 * @param transport the transport protocol to use
	 */
	void setTransport(RSocketServer.Transport transport);

	/**
	 * Sets the SSL configuration that will be applied to the server's default connector.
	 * @param ssl the SSL configuration
	 */
	void setSsl(@Nullable Ssl ssl);

	/**
	 * Sets an SSL bundle that can be used to get SSL configuration.
	 * @param sslBundles the SSL bundles
	 * @since 3.1.0
	 */
	void setSslBundles(@Nullable SslBundles sslBundles);

}
