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

package org.springframework.boot.reactor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.system.JavaVersion;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.ClassUtils;

/**
 * {@link EnvironmentPostProcessor} to enable the Reactor global features as early as
 * possible in the startup process.
 * <p>
 * If the "reactor-tools" dependency is available, the debug agent is enabled by default,
 * unless the {@code "spring.reactor.debug-agent.enabled"} configuration property is set
 * to false.
 * <p>
 * If the {@code "spring.threads.virtual.enabled"} property is enabled and the current JVM
 * is 21 or later, then the Reactor System property is set to configure the Bounded
 * Elastic Scheduler to use Virtual Threads globally.
 *
 * @author Brian Clozel
 * @since 3.2.0
 */
public class ReactorEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

	private static final String REACTOR_DEBUGAGENT_CLASS = "reactor.tools.agent.ReactorDebugAgent";

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		if (ClassUtils.isPresent(REACTOR_DEBUGAGENT_CLASS, null)) {
			Boolean agentEnabled = environment.getProperty("spring.reactor.debug-agent.enabled", Boolean.class);
			if (agentEnabled != Boolean.FALSE) {
				try {
					Class<?> debugAgent = Class.forName(REACTOR_DEBUGAGENT_CLASS);
					debugAgent.getMethod("init").invoke(null);
				}
				catch (Exception ex) {
					throw new RuntimeException("Failed to init Reactor's debug agent", ex);
				}
			}
		}
		if (environment.getProperty("spring.threads.virtual.enabled", boolean.class, false)
				&& JavaVersion.getJavaVersion().isEqualOrNewerThan(JavaVersion.TWENTY_ONE)) {
			System.setProperty("reactor.schedulers.defaultBoundedElasticOnVirtualThreads", "true");
		}
	}

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

}
