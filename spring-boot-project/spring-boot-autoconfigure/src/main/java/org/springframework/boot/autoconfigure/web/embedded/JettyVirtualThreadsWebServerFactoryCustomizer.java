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

package org.springframework.boot.autoconfigure.web.embedded;

import org.eclipse.jetty.util.VirtualThreads;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.web.embedded.jetty.ConfigurableJettyWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.core.Ordered;
import org.springframework.util.Assert;

/**
 * Activates virtual threads on the {@link ConfigurableJettyWebServerFactory}.
 *
 * @author Moritz Halbritter
 * @since 3.2.0
 */
public class JettyVirtualThreadsWebServerFactoryCustomizer
		implements WebServerFactoryCustomizer<ConfigurableJettyWebServerFactory>, Ordered {

	private final ServerProperties serverProperties;

	public JettyVirtualThreadsWebServerFactoryCustomizer(ServerProperties serverProperties) {
		this.serverProperties = serverProperties;
	}

	@Override
	public void customize(ConfigurableJettyWebServerFactory factory) {
		Assert.state(VirtualThreads.areSupported(), "Virtual threads are not supported");
		QueuedThreadPool threadPool = JettyThreadPool.create(this.serverProperties.getJetty().getThreads());
		threadPool.setVirtualThreadsExecutor(VirtualThreads.getDefaultVirtualThreadsExecutor());
		factory.setThreadPool(threadPool);
	}

	@Override
	public int getOrder() {
		return JettyWebServerFactoryCustomizer.ORDER + 1;
	}

}
