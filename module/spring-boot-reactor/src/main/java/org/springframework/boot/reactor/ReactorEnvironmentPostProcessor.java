/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.reactor;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.thread.Threading;
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
		if (Threading.VIRTUAL.isActive(environment)) {
			System.setProperty("reactor.schedulers.defaultBoundedElasticOnVirtualThreads", "true");
		}
	}

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

}
