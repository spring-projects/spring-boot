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

package org.springframework.boot.autoconfigure.elasticsearch.jest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import io.searchbox.action.Action;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.client.http.JestHttpClient;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchNodeTemplate;
import org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testsupport.runner.classpath.ClassPathOverrides;
import org.springframework.boot.testsupport.runner.classpath.ModifiedClassPathRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link JestAutoConfiguration}.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 */
@RunWith(ModifiedClassPathRunner.class)
@ClassPathOverrides("org.apache.logging.log4j:log4j-core:2.10.0")
public class JestAutoConfigurationTests {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(GsonAutoConfiguration.class, JestAutoConfiguration.class));

	@Before
	public void preventElasticsearchFromConfiguringNetty() {
		System.setProperty("es.set.netty.runtime.available.processors", "false");
	}

	@After
	public void close() {
		System.clearProperty("es.set.netty.runtime.available.processors");
	}

	@Test
	public void jestClientOnLocalhostByDefault() {
		this.contextRunner.run((context) -> assertThat(context.getBeansOfType(JestClient.class)).hasSize(1));
	}

	@Test
	public void customJestClient() {
		this.contextRunner.withUserConfiguration(CustomJestClient.class)
				.withPropertyValues("spring.elasticsearch.jest.uris[0]=http://localhost:9200")
				.run((context) -> assertThat(context.getBeansOfType(JestClient.class)).hasSize(1));
	}

	@Test
	public void customGson() {
		this.contextRunner.withUserConfiguration(CustomGson.class)
				.withPropertyValues("spring.elasticsearch.jest.uris=http://localhost:9200").run((context) -> {
					JestHttpClient client = (JestHttpClient) context.getBean(JestClient.class);
					assertThat(client.getGson()).isSameAs(context.getBean("customGson"));
				});
	}

	@Test
	public void customizerOverridesAutoConfig() {
		this.contextRunner.withUserConfiguration(BuilderCustomizer.class)
				.withPropertyValues("spring.elasticsearch.jest.uris=http://localhost:9200").run((context) -> {
					JestHttpClient client = (JestHttpClient) context.getBean(JestClient.class);
					assertThat(client.getGson()).isSameAs(context.getBean(BuilderCustomizer.class).getGson());
				});
	}

	@Test
	public void proxyHostWithoutPort() {
		this.contextRunner
				.withPropertyValues("spring.elasticsearch.jest.uris=http://localhost:9200",
						"spring.elasticsearch.jest.proxy.host=proxy.example.com")
				.run((context) -> assertThat(context.getStartupFailure()).isInstanceOf(BeanCreationException.class)
						.hasMessageContaining("Proxy port must not be null"));
	}

	@Test
	public void jestCanCommunicateWithElasticsearchInstance() {
		new ElasticsearchNodeTemplate().doWithNode((node) -> this.contextRunner
				.withPropertyValues("spring.elasticsearch.jest.uris=http://localhost:" + node.getHttpPort())
				.run((context) -> {
					JestClient client = context.getBean(JestClient.class);
					Map<String, String> source = new HashMap<>();
					source.put("a", "alpha");
					source.put("b", "bravo");
					Index index = new Index.Builder(source).index("foo").type("bar").build();
					execute(client, index);
					SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
					searchSourceBuilder.query(QueryBuilders.matchQuery("a", "alpha"));
					assertThat(
							execute(client, new Search.Builder(searchSourceBuilder.toString()).addIndex("foo").build())
									.getResponseCode()).isEqualTo(200);
				}));
	}

	private JestResult execute(JestClient client, Action<? extends JestResult> action) {
		for (int i = 0; i < 2; i++) {
			try {
				return client.execute(action);
			}
			catch (IOException ex) {
				// Continue
			}
		}
		try {
			return client.execute(action);
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
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

	@Configuration
	@Import(CustomGson.class)
	static class BuilderCustomizer {

		private final Gson gson = new Gson();

		@Bean
		public HttpClientConfigBuilderCustomizer customizer() {
			return (builder) -> builder.gson(BuilderCustomizer.this.gson);
		}

		Gson getGson() {
			return this.gson;
		}

	}

}
