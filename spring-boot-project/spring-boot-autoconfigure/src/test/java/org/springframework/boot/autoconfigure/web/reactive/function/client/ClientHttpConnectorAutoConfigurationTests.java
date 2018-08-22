/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.web.reactive.function.client;

import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.client.reactive.ReactorResourceFactory;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link ClientHttpConnectorAutoConfiguration}
 *
 * @author Brian Clozel
 */
public class ClientHttpConnectorAutoConfigurationTests {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(
					AutoConfigurations.of(ClientHttpConnectorAutoConfiguration.class));

	@Test
	public void shouldCreateHttpClientBeans() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(ReactorResourceFactory.class);
			assertThat(context).hasSingleBean(ReactorClientHttpConnector.class);
			WebClientCustomizer clientCustomizer = context
					.getBean(WebClientCustomizer.class);
			WebClient.Builder builder = mock(WebClient.Builder.class);
			clientCustomizer.customize(builder);
			verify(builder, times(1))
					.clientConnector(any(ReactorClientHttpConnector.class));
		});
	}

}
