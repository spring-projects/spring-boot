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

import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.web.embedded.jetty.ConfigurableJettyWebServerFactory;

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
		ServerProperties properties = new ServerProperties();
		JettyVirtualThreadsWebServerFactoryCustomizer customizer = new JettyVirtualThreadsWebServerFactoryCustomizer(
				properties);
		ConfigurableJettyWebServerFactory factory = mock(ConfigurableJettyWebServerFactory.class);
		customizer.customize(factory);
		then(factory).should().setThreadPool(assertArg((threadPool) -> {
			assertThat(threadPool).isInstanceOf(QueuedThreadPool.class);
			QueuedThreadPool queuedThreadPool = (QueuedThreadPool) threadPool;
			assertThat(queuedThreadPool.getVirtualThreadsExecutor()).isNotNull();
		}));
	}

}
