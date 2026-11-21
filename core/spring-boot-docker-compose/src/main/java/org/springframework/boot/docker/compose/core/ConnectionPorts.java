/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docker.compose.core;

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Provides access to the ports that can be used to connect to a {@link RunningService}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 3.1.0
 * @see RunningService
 */
public interface ConnectionPorts {

	/**
	 * Return the host port mapped to the given container port.
	 * @param containerPort the container port. This is usually the standard port for the
	 * service (e.g. port 80 for HTTP)
	 * @return the host port. This can be an ephemeral port that is different from the
	 * container port
	 * @throws IllegalStateException if the container port is not mapped or if the port is
	 * mapped for multiple protocols.
	 * @deprecated since 4.0.11 for removal in 4.4.0 in favor of {@link #get(int, String)}
	 */
	@Deprecated(since = "4.0.11", forRemoval = true)
	int get(int containerPort);

	/**
	 * Return the host port mapped to the given TCP container port.
	 * @param containerPort the container port. This is usually the standard port for the
	 * service (e.g. port 80 for HTTP)
	 * @return the host port. This can be an ephemeral port that is different from the
	 * container port
	 * @throws IllegalStateException if the container port is not mapped or if the port is
	 * mapped for a different protocol.
	 * @since 4.0.11
	 * @see #get(int, String)
	 */
	default int getTcp(int containerPort) {
		return get(containerPort, "tcp");
	}

	/**
	 * Return the host port mapped to the given UDP container port.
	 * @param containerPort the container port. This is usually the standard port for the
	 * service (e.g. port 80 for HTTP)
	 * @return the host port. This can be an ephemeral port that is different from the
	 * container port
	 * @throws IllegalStateException if the container port is not mapped or if the port is
	 * mapped for a different protocol.
	 * @since 4.0.11
	 * @see #get(int, String)
	 */
	default int getUdp(int containerPort) {
		return get(containerPort, "udp");
	}

	/**
	 * Return the host port mapped to the given container port.
	 * @param containerPort the container port. This is usually the standard port for the
	 * service (e.g. port 80 for HTTP)
	 * @param protocol the connection protocol
	 * @return the host port. This can be an ephemeral port that is different from the
	 * container port
	 * @throws IllegalStateException if the container port is not mapped or if the port is
	 * mapped for a different protocol.
	 * @since 4.0.11
	 * @see #getTcp(int)
	 * @see #getUdp(int)
	 */
	int get(int containerPort, String protocol);

	/**
	 * Return all host ports in use.
	 * @return a list of all host ports
	 * @see #getAll(String)
	 */
	List<Integer> getAll();

	/**
	 * Return all host ports in use that match the given protocol.
	 * @param protocol the protocol in use (for example 'tcp') or {@code null} to return
	 * all host ports
	 * @return a list of all host ports using the given protocol
	 */
	List<Integer> getAll(@Nullable String protocol);

}
