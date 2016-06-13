/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.autoconfigure.jest;

import com.google.gson.Gson;
import io.searchbox.client.JestClient;
import io.searchbox.client.http.JestHttpClient;
import org.junit.After;
import org.junit.Test;

import org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link JestAutoConfiguration}.
 *
 * @author Stephane Nicoll
 */
public class JestAutoConfigurationTests {

	protected AnnotationConfigApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void jestClientOnLocalhostByDefault() {
		load();
		assertThat(this.context.getBeansOfType(JestClient.class)).hasSize(1);
	}

	@Test
	public void customJestClient() {
		load(CustomJestClient.class, "spring.jest.uris=http://localhost:9200");
		assertThat(this.context.getBeansOfType(JestClient.class)).hasSize(1);
	}

	@Test
	public void customGson() {
		load(CustomGson.class, "spring.jest.uris=http://localhost:9200");
		JestHttpClient client = (JestHttpClient) this.context.getBean(JestClient.class);
		assertThat(client.getGson()).isSameAs(this.context.getBean("customGson"));
	}

	private void load(String... environment) {
		load(null, environment);
	}

	private void load(Class<?> config, String... environment) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(context, environment);
		if (config != null) {
			context.register(config);
		}
		context.register(GsonAutoConfiguration.class, JestAutoConfiguration.class);
		context.refresh();
		this.context = context;
	}

	@Configuration
	static class CustomJestClient {

		@Bean
		public JestClient customJestClient() {
			return mock(JestClient.class);
		}

	}

	@Configuration
	static class CustomGson {

		@Bean
		public Gson customGson() {
			return new Gson();
		}

	}

}
