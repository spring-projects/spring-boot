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

package org.springframework.boot.jetty.autoconfigure;

import org.eclipse.jetty.util.VirtualThreads;
import org.eclipse.jetty.util.thread.VirtualThreadPool;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.jetty.ConfigurableJettyWebServerFactory;
import org.springframework.boot.jetty.autoconfigure.JettyServerProperties.Threads;
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
		VirtualThreadPool virtualThreadPool = new VirtualThreadPool();
		virtualThreadPool.setName("jetty-");
		if (this.serverProperties != null) {
			Threads properties = this.serverProperties.getThreads();
			int maxThreadCount = (properties.getMax() > 0) ? properties.getMax() : 200;
			virtualThreadPool.setMaxThreads(maxThreadCount);
		}
		factory.setThreadPool(virtualThreadPool);
	}

	@Override
	public int getOrder() {
		return JettyWebServerFactoryCustomizer.ORDER + 1;
	}

}
