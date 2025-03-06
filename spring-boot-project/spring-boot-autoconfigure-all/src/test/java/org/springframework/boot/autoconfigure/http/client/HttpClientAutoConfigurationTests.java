/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.autoconfigure.http.client;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings.Redirects;
import org.springframework.boot.http.client.SimpleClientHttpRequestFactoryBuilder;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HttpClientAutoConfiguration}.
 *
 * @author Phillip Webb
 */
class HttpClientAutoConfigurationTests {

	private static final AutoConfigurations autoConfigurations = AutoConfigurations
		.of(HttpClientAutoConfiguration.class, SslAutoConfiguration.class);

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(autoConfigurations);

	@Test
	void configuresDetectedClientHttpRequestFactoryBuilder() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(ClientHttpRequestFactoryBuilder.class));
	}

	@Test
	void configuresDefinedClientHttpRequestFactoryBuilder() {
		this.contextRunner.withPropertyValues("spring.http.client.factory=simple")
			.run((context) -> assertThat(context.getBean(ClientHttpRequestFactoryBuilder.class))
				.isInstanceOf(SimpleClientHttpRequestFactoryBuilder.class));
	}

	@Test
	void configuresClientHttpRequestFactorySettings() {
		this.contextRunner.withPropertyValues(sslPropertyValues().toArray(String[]::new))
			.withPropertyValues("spring.http.client.redirects=dont-follow", "spring.http.client.connect-timeout=10s",
					"spring.http.client.read-timeout=20s", "spring.http.client.ssl.bundle=test")
			.run((context) -> {
				ClientHttpRequestFactorySettings settings = context.getBean(ClientHttpRequestFactorySettings.class);
				assertThat(settings.redirects()).isEqualTo(Redirects.DONT_FOLLOW);
				assertThat(settings.connectTimeout()).isEqualTo(Duration.ofSeconds(10));
				assertThat(settings.readTimeout()).isEqualTo(Duration.ofSeconds(20));
				assertThat(settings.sslBundle().getKey().getAlias()).isEqualTo("alias1");
			});
	}

	private List<String> sslPropertyValues() {
		List<String> propertyValues = new ArrayList<>();
		String location = "classpath:org/springframework/boot/autoconfigure/ssl/";
		propertyValues.add("spring.ssl.bundle.pem.test.key.alias=alias1");
		propertyValues.add("spring.ssl.bundle.pem.test.truststore.type=PKCS12");
		propertyValues.add("spring.ssl.bundle.pem.test.truststore.certificate=" + location + "rsa-cert.pem");
		propertyValues.add("spring.ssl.bundle.pem.test.truststore.private-key=" + location + "rsa-key.pem");
		return propertyValues;
	}

	@Test
	void whenReactiveWebApplicationBeansAreNotConfigured() {
		new ReactiveWebApplicationContextRunner().withConfiguration(autoConfigurations)
			.run((context) -> assertThat(context).doesNotHaveBean(ClientHttpRequestFactoryBuilder.class)
				.doesNotHaveBean(ClientHttpRequestFactorySettings.class));
	}

}
