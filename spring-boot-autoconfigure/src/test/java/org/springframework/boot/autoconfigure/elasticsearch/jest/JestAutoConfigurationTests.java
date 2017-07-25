/*
 * Copyright 2012-2017 the original author or authors.
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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchNodeTemplate;
import org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
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
public class JestAutoConfigurationTests {

	@Before
	public void preventElasticsearchFromConfiguringNetty() {
		System.setProperty("es.set.netty.runtime.available.processors", "false");
	}

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	protected AnnotationConfigApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
		System.clearProperty("es.set.netty.runtime.available.processors");
	}

	@Test
	public void jestClientOnLocalhostByDefault() {
		load();
		assertThat(this.context.getBeansOfType(JestClient.class)).hasSize(1);
	}

	@Test
	public void customJestClient() {
		load(CustomJestClient.class,
				"spring.elasticsearch.jest.uris[0]=http://localhost:9200");
		assertThat(this.context.getBeansOfType(JestClient.class)).hasSize(1);
	}

	@Test
	public void customGson() {
		load(CustomGson.class, "spring.elasticsearch.jest.uris=http://localhost:9200");
		JestHttpClient client = (JestHttpClient) this.context.getBean(JestClient.class);
		assertThat(client.getGson()).isSameAs(this.context.getBean("customGson"));
	}

	@Test
	public void customizerOverridesAutoConfig() {
		load(BuilderCustomizer.class,
				"spring.elasticsearch.jest.uris=http://localhost:9200");
		JestHttpClient client = (JestHttpClient) this.context.getBean(JestClient.class);
		assertThat(client.getGson())
				.isSameAs(this.context.getBean(BuilderCustomizer.class).getGson());
	}

	@Test
	public void proxyHostWithoutPort() {
		this.thrown.expect(BeanCreationException.class);
		this.thrown.expectMessage("Proxy port must not be null");
		load("spring.elasticsearch.jest.uris=http://localhost:9200",
				"spring.elasticsearch.jest.proxy.host=proxy.example.com");
	}

	@Test
	public void jestCanCommunicateWithElasticsearchInstance() throws IOException {
		new ElasticsearchNodeTemplate().doWithNode((node) -> {
			load("spring.elasticsearch.jest.uris=http://localhost:" + node.getHttpPort());
			JestClient client = this.context.getBean(JestClient.class);
			Map<String, String> source = new HashMap<>();
			source.put("a", "alpha");
			source.put("b", "bravo");
			Index index = new Index.Builder(source).index("foo").type("bar").build();
			execute(client, index);
			SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
			searchSourceBuilder.query(QueryBuilders.matchQuery("a", "alpha"));
			assertThat(
					execute(client,
							new Search.Builder(searchSourceBuilder.toString())
									.addIndex("foo").build()).getResponseCode())
											.isEqualTo(200);
		});
	}

	private JestResult execute(JestClient client, Action<? extends JestResult> action) {
		try {
			return client.execute(action);
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	private void load(String... environment) {
		load(null, environment);
	}

	private void load(Class<?> config, String... environment) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of(environment).applyTo(context);
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
