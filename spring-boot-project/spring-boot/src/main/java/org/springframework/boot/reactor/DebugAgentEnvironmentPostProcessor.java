/*
 * Copyright 2012-2019 the original author or authors.
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
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.ClassUtils;

/**
 * {@link EnvironmentPostProcessor} to enable the Reactor Debug Agent if available.
 * <p>
 * The debug agent is enabled by default, unless the
 * {@code "spring.reactor.debug-agent.enabled"} configuration property is set to false. We
 * are using here an {@link EnvironmentPostProcessor} instead of an auto-configuration
 * class to enable the agent as soon as possible during the startup process.
 *
 * @author Brian Clozel
 * @since 2.2.0
 */
public class DebugAgentEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

	private static final String REACTOR_DEBUGAGENT_CLASS = "reactor.tools.agent.ReactorDebugAgent";

	private static final String DEBUGAGENT_ENABLED_CONFIG_KEY = "spring.reactor.debug-agent.enabled";

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		if (ClassUtils.isPresent(REACTOR_DEBUGAGENT_CLASS, null)) {
			Boolean agentEnabled = environment.getProperty(DEBUGAGENT_ENABLED_CONFIG_KEY, Boolean.class);
			if (agentEnabled != Boolean.FALSE) {
				try {
					Class<?> debugAgent = Class.forName(REACTOR_DEBUGAGENT_CLASS);
					debugAgent.getMethod("init").invoke(null);
				}
				catch (Exception ex) {
					throw new RuntimeException("Failed to init Reactor's debug agent");
				}
			}
		}
	}

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

}
