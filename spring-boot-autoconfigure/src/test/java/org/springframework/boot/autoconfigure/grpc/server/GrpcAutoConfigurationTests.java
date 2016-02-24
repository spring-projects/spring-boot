/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.autoconfigure.grpc.server;

import java.io.IOException;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link GrpcServerAutoConfiguration}.
 *
 * @author Ray Tsang
 */
public class GrpcAutoConfigurationTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	private AnnotationConfigApplicationContext context;

	@After
	public void closeContext() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void defaultConfiguration() throws IOException {
		load();
		GrpcServerProperties properties = this.context
				.getBean(GrpcServerProperties.class);
		GrpcServerLifecycle lifecycle = this.context.getBean(GrpcServerLifecycle.class);

		assertThat(properties.getAddress()).isEqualTo("0.0.0.0");
		assertThat(properties.getPort()).isEqualByComparingTo(9443);
		assertThat(lifecycle).isNotNull();
	}

	@Test
	public void configInstanceCustomProperties() {
		load(TestConfiguration.class);
		GrpcServerProperties properties = this.context
				.getBean(GrpcServerProperties.class);
		GrpcServerLifecycle lifecycle = this.context.getBean(GrpcServerLifecycle.class);

		assertThat(properties.getAddress()).isEqualTo("127.0.0.1");
		assertThat(properties.getPort()).isEqualByComparingTo(12345);
		assertThat(lifecycle).isNotNull();
	}

	private void load(String... environment) {
		load(null, environment);
	}

	private void load(Class<?> config, String... environment) {
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(applicationContext, environment);
		if (config != null) {
			applicationContext.register(config);
		}
		applicationContext.register(GrpcServerAutoConfiguration.class);
		applicationContext.refresh();
		this.context = applicationContext;
	}

	@Configuration
	static class TestConfiguration {
		@Bean
		GrpcServerProperties myGrpcProperties() {
			GrpcServerProperties props = new GrpcServerProperties();
			props.setPort(12345);
			props.setAddress("127.0.0.1");
			return props;
		}
	}
}
