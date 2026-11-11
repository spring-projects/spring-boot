/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jetty.autoconfigure;

import org.eclipse.jetty.util.VirtualThreads;
import org.eclipse.jetty.util.thread.VirtualThreadPool;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.jetty.ConfigurableJettyWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.core.Ordered;
import org.springframework.util.Assert;

/**
 * Activates virtual threads on the {@link ConfigurableJettyWebServerFactory}.
 *
 * @author Moritz Halbritter
 * @author Brian Clozel
 * @since 4.0.0
 */
public class JettyVirtualThreadsWebServerFactoryCustomizer
		implements WebServerFactoryCustomizer<ConfigurableJettyWebServerFactory>, Ordered {

	private final @Nullable JettyServerProperties serverProperties;

	/**
	 * Create a new JettyVirtualThreadsWebServerFactoryCustomizer.
	 * @deprecated since 4.0.3 for removal in 4.3.0 in favor of
	 * {@link #JettyVirtualThreadsWebServerFactoryCustomizer(JettyServerProperties)}
	 */
	@Deprecated(since = "4.0.3", forRemoval = true)
	// Suppress the null passing here as we don't want to put @Nullable on the
	// JettyServerProperties in the other constructor
	@SuppressWarnings("NullAway")
	public JettyVirtualThreadsWebServerFactoryCustomizer() {
		this(null);
	}

	/**
	 * Create a new JettyVirtualThreadsWebServerFactoryCustomizer.
	 * @param serverProperties the server properties
	 */
	public JettyVirtualThreadsWebServerFactoryCustomizer(JettyServerProperties serverProperties) {
		this.serverProperties = serverProperties;
	}

	@Override
	public void customize(ConfigurableJettyWebServerFactory factory) {
		Assert.state(VirtualThreads.areSupported(), "Virtual threads are not supported");
		Integer maxTasks = getMaxTasks();
		VirtualThreadPool virtualThreadPool = (maxTasks != null) ? new VirtualThreadPool(maxTasks)
				: new VirtualThreadPool();
		virtualThreadPool.setName("jetty-");
		factory.setThreadPool(virtualThreadPool);
	}

	private @Nullable Integer getMaxTasks() {
		if (this.serverProperties == null) {
			return null;
		}
		Integer maxThreads = this.serverProperties.getThreads().getMax();
		return (maxThreads > 0) ? maxThreads : null;
	}

	@Override
	public int getOrder() {
		return JettyWebServerFactoryCustomizer.ORDER + 1;
	}

}
