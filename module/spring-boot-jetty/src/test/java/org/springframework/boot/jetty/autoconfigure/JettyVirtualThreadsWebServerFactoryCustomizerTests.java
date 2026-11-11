/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jetty.autoconfigure;

import org.eclipse.jetty.util.thread.VirtualThreadPool;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import org.springframework.boot.jetty.ConfigurableJettyWebServerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link JettyVirtualThreadsWebServerFactoryCustomizer}.
 *
 * @author Moritz Halbritter
 */
class JettyVirtualThreadsWebServerFactoryCustomizerTests {

	@Test
	@EnabledForJreRange(min = JRE.JAVA_21)
	void shouldConfigureVirtualThreads() {
		JettyServerProperties serverProperties = new JettyServerProperties();
		serverProperties.getThreads().setMax(100);
		JettyVirtualThreadsWebServerFactoryCustomizer customizer = new JettyVirtualThreadsWebServerFactoryCustomizer(
				serverProperties);
		ConfigurableJettyWebServerFactory factory = mock(ConfigurableJettyWebServerFactory.class);
		customizer.customize(factory);
		then(factory).should().setThreadPool(assertArg((threadPool) -> {
			assertThat(threadPool).isInstanceOf(VirtualThreadPool.class);
			VirtualThreadPool virtualThreadPool = (VirtualThreadPool) threadPool;
			assertThat(virtualThreadPool.getName()).isEqualTo("jetty-");
			assertThat(virtualThreadPool.getMaxConcurrentTasks()).isEqualTo(100);
		}));
	}

}
