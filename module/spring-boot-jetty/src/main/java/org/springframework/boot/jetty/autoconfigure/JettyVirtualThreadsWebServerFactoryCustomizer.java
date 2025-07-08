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
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.jetty.ConfigurableJettyWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;

/**
 * Activates virtual threads on the {@link ConfigurableJettyWebServerFactory}.
 *
 * @author Moritz Halbritter
 * @since 4.0.0
 */
public class JettyVirtualThreadsWebServerFactoryCustomizer
		implements WebServerFactoryCustomizer<ConfigurableJettyWebServerFactory>, Ordered, EnvironmentAware {

	private final JettyServerProperties jettyProperties;

	private final boolean bind;

	public JettyVirtualThreadsWebServerFactoryCustomizer(JettyServerProperties jettyProperties) {
		this.jettyProperties = jettyProperties;
		this.bind = false;
	}

	@Override
	public void customize(ConfigurableJettyWebServerFactory factory) {
		Assert.state(VirtualThreads.areSupported(), "Virtual threads are not supported");
		QueuedThreadPool threadPool = JettyThreadPool.create(this.jettyProperties.getThreads());
		threadPool.setVirtualThreadsExecutor(VirtualThreads.getNamedVirtualThreadsExecutor("jetty-"));
		factory.setThreadPool(threadPool);
	}

	@Override
	public int getOrder() {
		return JettyWebServerFactoryCustomizer.ORDER + 1;
	}

	@Override
	public void setEnvironment(Environment environment) {
		if (this.bind) {
			Binder.get(environment).bind("server.jetty", Bindable.ofInstance(this.jettyProperties));
		}
	}

}
