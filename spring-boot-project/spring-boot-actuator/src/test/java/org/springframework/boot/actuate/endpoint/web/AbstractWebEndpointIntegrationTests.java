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

package org.springframework.boot.actuate.endpoint.web;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.junit.Test;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.boot.actuate.endpoint.annotation.DeleteOperation;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.boot.actuate.endpoint.convert.ConversionServiceParameterMapper;
import org.springframework.boot.actuate.endpoint.reflect.ParameterMapper;
import org.springframework.boot.actuate.endpoint.web.annotation.WebAnnotationEndpointDiscoverer;
import org.springframework.boot.web.embedded.tomcat.TomcatEmbeddedWebappClassLoader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Abstract base class for web endpoint integration tests.
 *
 * @param <T> the type of application context used by the tests
 * @author Andy Wilkinson
 */
public abstract class AbstractWebEndpointIntegrationTests<T extends ConfigurableApplicationContext> {

	private static final Duration TIMEOUT = Duration.ofMinutes(6);

	private static final String ACTUATOR_MEDIA_TYPE_PATTERN = "application/vnd.test\\+json(;charset=UTF-8)?";

	private static final String JSON_MEDIA_TYPE_PATTERN = "application/json(;charset=UTF-8)?";

	private final Class<?> exporterConfiguration;

	protected AbstractWebEndpointIntegrationTests(Class<?> exporterConfiguration) {
		this.exporterConfiguration = exporterConfiguration;
	}

	@Test
	public void readOperation() {
		load(TestEndpointConfiguration.class,
				(client) -> client.get().uri("/test").exchange().expectStatus().isOk()
						.expectBody().jsonPath("All").isEqualTo(true));
	}

	@Test
	public void readOperationWithEndpointsMappedToTheRoot() {
		load(TestEndpointConfiguration.class, "",
				(client) -> client.get().uri("/test").exchange().expectStatus().isOk()
						.expectBody().jsonPath("All").isEqualTo(true));
	}

	@Test
	public void readOperationWithSelector() {
		load(TestEndpointConfiguration.class,
				(client) -> client.get().uri("/test/one").exchange().expectStatus().isOk()
						.expectBody().jsonPath("part").isEqualTo("one"));
	}

	@Test
	public void readOperationWithSelectorContainingADot() {
		load(TestEndpointConfiguration.class,
				(client) -> client.get().uri("/test/foo.bar").exchange().expectStatus()
						.isOk().expectBody().jsonPath("part").isEqualTo("foo.bar"));
	}

	@Test
	public void linksToOtherEndpointsAreProvided() {
		load(TestEndpointConfiguration.class,
				(client) -> client.get().uri("").exchange().expectStatus().isOk()
						.expectBody().jsonPath("_links.length()").isEqualTo(3)
						.jsonPath("_links.self.href").isNotEmpty()
						.jsonPath("_links.self.templated").isEqualTo(false)
						.jsonPath("_links.test.href").isNotEmpty()
						.jsonPath("_links.test.templated").isEqualTo(false)
						.jsonPath("_links.test-part.href").isNotEmpty()
						.jsonPath("_links.test-part.templated").isEqualTo(true));
	}

	@Test
	public void linksMappingIsDisabledWhenEndpointPathIsEmpty() {
		load(TestEndpointConfiguration.class, "",
				(client) -> client.get().uri("").exchange().expectStatus().isNotFound());
	}

	@Test
	public void readOperationWithSingleQueryParameters() {
		load(QueryEndpointConfiguration.class,
				(client) -> client.get().uri("/query?one=1&two=2").exchange()
						.expectStatus().isOk().expectBody().jsonPath("query")
						.isEqualTo("1 2"));
	}

	@Test
	public void readOperationWithSingleQueryParametersAndMultipleValues() {
		load(QueryEndpointConfiguration.class,
				(client) -> client.get().uri("/query?one=1&one=1&two=2").exchange()
						.expectStatus().isOk().expectBody().jsonPath("query")
						.isEqualTo("1,1 2"));
	}

	@Test
	public void readOperationWithListQueryParameterAndSingleValue() {
		load(QueryWithListEndpointConfiguration.class,
				(client) -> client.get().uri("/query?one=1&two=2").exchange()
						.expectStatus().isOk().expectBody().jsonPath("query")
						.isEqualTo("1 [2]"));
	}

	@Test
	public void readOperationWithListQueryParameterAndMultipleValues() {
		load(QueryWithListEndpointConfiguration.class,
				(client) -> client.get().uri("/query?one=1&two=2&two=2").exchange()
						.expectStatus().isOk().expectBody().jsonPath("query")
						.isEqualTo("1 [2, 2]"));
	}

	@Test
	public void readOperationWithMappingFailureProducesBadRequestResponse() {
		load(QueryEndpointConfiguration.class, (client) -> client.get()
				.uri("/query?two=two").exchange().expectStatus().isBadRequest());
	}

	@Test
	public void writeOperation() {
		load(TestEndpointConfiguration.class, (client) -> {
			Map<String, Object> body = new HashMap<>();
			body.put("foo", "one");
			body.put("bar", "two");
			client.post().uri("/test").syncBody(body).exchange().expectStatus()
					.isNoContent().expectBody().isEmpty();
		});
	}

	@Test
	public void writeOperationWithVoidResponse() {
		load(VoidWriteResponseEndpointConfiguration.class, (context, client) -> {
			client.post().uri("/voidwrite").exchange().expectStatus().isNoContent()
					.expectBody().isEmpty();
			verify(context.getBean(EndpointDelegate.class)).write();
		});
	}

	@Test
	public void deleteOperation() {
		load(TestEndpointConfiguration.class,
				(client) -> client.delete().uri("/test/one").exchange().expectStatus()
						.isOk().expectBody().jsonPath("part").isEqualTo("one"));
	}

	@Test
	public void deleteOperationWithVoidResponse() {
		load(VoidDeleteResponseEndpointConfiguration.class, (context, client) -> {
			client.delete().uri("/voiddelete").exchange().expectStatus().isNoContent()
					.expectBody().isEmpty();
			verify(context.getBean(EndpointDelegate.class)).delete();
		});
	}

	@Test
	public void nullIsPassedToTheOperationWhenArgumentIsNotFoundInPostRequestBody() {
		load(TestEndpointConfiguration.class, (context, client) -> {
			Map<String, Object> body = new HashMap<>();
			body.put("foo", "one");
			client.post().uri("/test").syncBody(body).exchange().expectStatus()
					.isNoContent().expectBody().isEmpty();
			verify(context.getBean(EndpointDelegate.class)).write("one", null);
		});
	}

	@Test
	public void nullsArePassedToTheOperationWhenPostRequestHasNoBody() {
		load(TestEndpointConfiguration.class, (context, client) -> {
			client.post().uri("/test").contentType(MediaType.APPLICATION_JSON).exchange()
					.expectStatus().isNoContent().expectBody().isEmpty();
			verify(context.getBean(EndpointDelegate.class)).write(null, null);
		});
	}

	@Test
	public void nullResponseFromReadOperationResultsInNotFoundResponseStatus() {
		load(NullReadResponseEndpointConfiguration.class, (context, client) -> client
				.get().uri("/nullread").exchange().expectStatus().isNotFound());
	}

	@Test
	public void nullResponseFromDeleteOperationResultsInNoContentResponseStatus() {
		load(NullDeleteResponseEndpointConfiguration.class, (context, client) -> client
				.delete().uri("/nulldelete").exchange().expectStatus().isNoContent());
	}

	@Test
	public void nullResponseFromWriteOperationResultsInNoContentResponseStatus() {
		load(NullWriteResponseEndpointConfiguration.class, (context, client) -> client
				.post().uri("/nullwrite").exchange().expectStatus().isNoContent());
	}

	@Test
	public void readOperationWithResourceResponse() {
		load(ResourceEndpointConfiguration.class, (context, client) -> {
			byte[] responseBody = client.get().uri("/resource").exchange().expectStatus()
					.isOk().expectHeader().contentType(MediaType.APPLICATION_OCTET_STREAM)
					.returnResult(byte[].class).getResponseBodyContent();
			assertThat(responseBody).containsExactly(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
		});
	}

	@Test
	public void readOperationWithResourceWebOperationResponse() {
		load(ResourceWebEndpointResponseEndpointConfiguration.class,
				(context, client) -> {
					byte[] responseBody = client.get().uri("/resource").exchange()
							.expectStatus().isOk().expectHeader()
							.contentType(MediaType.APPLICATION_OCTET_STREAM)
							.returnResult(byte[].class).getResponseBodyContent();
					assertThat(responseBody).containsExactly(0, 1, 2, 3, 4, 5, 6, 7, 8,
							9);
				});
	}

	@Test
	public void readOperationWithMonoResponse() {
		load(MonoResponseEndpointConfiguration.class,
				(client) -> client.get().uri("/mono").exchange().expectStatus().isOk()
						.expectBody().jsonPath("a").isEqualTo("alpha"));
	}

	@Test
	public void readOperationWithCustomMediaType() {
		load(CustomMediaTypesEndpointConfiguration.class,
				(client) -> client.get().uri("/custommediatypes").exchange()
						.expectStatus().isOk().expectHeader()
						.valueMatches("Content-Type", "text/plain(;charset=.*)?"));
	}

	@Test
	public void readOperationWithMissingRequiredParametersReturnsBadRequestResponse()
			throws Exception {
		load(RequiredParameterEndpointConfiguration.class, (client) -> client.get()
				.uri("/requiredparameters").exchange().expectStatus().isBadRequest());
	}

	@Test
	public void readOperationWithMissingNullableParametersIsOk() throws Exception {
		load(RequiredParameterEndpointConfiguration.class, (client) -> client.get()
				.uri("/requiredparameters?foo=hello").exchange().expectStatus().isOk());
	}

	@Test
	public void endpointsProducePrimaryMediaTypeByDefault() {
		load(TestEndpointConfiguration.class,
				(client) -> client.get().uri("/test").exchange().expectStatus().isOk()
						.expectHeader()
						.valueMatches("Content-Type", ACTUATOR_MEDIA_TYPE_PATTERN));
	}

	@Test
	public void endpointsProduceSecondaryMediaTypeWhenRequested() {
		load(TestEndpointConfiguration.class,
				(client) -> client.get().uri("/test").accept(MediaType.APPLICATION_JSON)
						.exchange().expectStatus().isOk().expectHeader()
						.valueMatches("Content-Type", JSON_MEDIA_TYPE_PATTERN));
	}

	@Test
	public void linksProducesPrimaryMediaTypeByDefault() {
		load(TestEndpointConfiguration.class,
				(client) -> client.get().uri("").exchange().expectStatus().isOk()
						.expectHeader()
						.valueMatches("Content-Type", ACTUATOR_MEDIA_TYPE_PATTERN));
	}

	@Test
	public void linksProducesSecondaryMediaTypeWhenRequested() {
		load(TestEndpointConfiguration.class,
				(client) -> client.get().uri("").accept(MediaType.APPLICATION_JSON)
						.exchange().expectStatus().isOk().expectHeader()
						.valueMatches("Content-Type", JSON_MEDIA_TYPE_PATTERN));
	}

	protected abstract T createApplicationContext(Class<?>... config);

	protected abstract int getPort(T context);

	private void load(Class<?> configuration,
			BiConsumer<ApplicationContext, WebTestClient> consumer) {
		load(configuration, "/endpoints", consumer);
	}

	private void load(Class<?> configuration, String endpointPath,
			BiConsumer<ApplicationContext, WebTestClient> consumer) {
		T context = createApplicationContext(configuration, this.exporterConfiguration);
		context.getEnvironment().getPropertySources().addLast(new MapPropertySource(
				"test", Collections.singletonMap("endpointPath", endpointPath)));
		context.refresh();
		try {
			String url = "http://localhost:" + getPort(context) + endpointPath;
			consumer.accept(context, WebTestClient.bindToServer().baseUrl(url)
					.responseTimeout(TIMEOUT).build());
		}
		finally {
			context.close();
		}
	}

	protected void load(Class<?> configuration, Consumer<WebTestClient> clientConsumer) {
		load(configuration, "/endpoints",
				(context, client) -> clientConsumer.accept(client));
	}

	protected void load(Class<?> configuration, String endpointPath,
			Consumer<WebTestClient> clientConsumer) {
		load(configuration, endpointPath,
				(context, client) -> clientConsumer.accept(client));
	}

	@Configuration
	static class BaseConfiguration {

		@Bean
		public EndpointDelegate endpointDelegate() {
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
			if (classLoader instanceof TomcatEmbeddedWebappClassLoader) {
				Thread.currentThread().setContextClassLoader(classLoader.getParent());
			}
			try {
				return mock(EndpointDelegate.class);
			}
			finally {
				Thread.currentThread().setContextClassLoader(classLoader);
			}
		}

		@Bean
		public EndpointMediaTypes endpointMediaTypes() {
			List<String> mediaTypes = Arrays.asList("application/vnd.test+json",
					"application/json");
			return new EndpointMediaTypes(mediaTypes, mediaTypes);
		}

		@Bean
		public WebAnnotationEndpointDiscoverer webEndpointDiscoverer(
				ApplicationContext applicationContext) {
			ParameterMapper parameterMapper = new ConversionServiceParameterMapper(
					DefaultConversionService.getSharedInstance());
			return new WebAnnotationEndpointDiscoverer(applicationContext,
					parameterMapper, endpointMediaTypes(),
					EndpointPathResolver.useEndpointId(), null, null);
		}

		@Bean
		public PropertyPlaceholderConfigurer propertyPlaceholderConfigurer() {
			return new PropertyPlaceholderConfigurer();
		}

	}

	@Configuration
	@Import(BaseConfiguration.class)
	protected static class TestEndpointConfiguration {

		@Bean
		public TestEndpoint testEndpoint(EndpointDelegate endpointDelegate) {
			return new TestEndpoint(endpointDelegate);
		}

	}

	@Configuration
	@Import(BaseConfiguration.class)
	static class QueryEndpointConfiguration {

		@Bean
		public QueryEndpoint queryEndpoint() {
			return new QueryEndpoint();
		}

	}

	@Configuration
	@Import(BaseConfiguration.class)
	static class QueryWithListEndpointConfiguration {

		@Bean
		public QueryWithListEndpoint queryEndpoint() {
			return new QueryWithListEndpoint();
		}

	}

	@Configuration
	@Import(BaseConfiguration.class)
	static class VoidWriteResponseEndpointConfiguration {

		@Bean
		public VoidWriteResponseEndpoint voidWriteResponseEndpoint(
				EndpointDelegate delegate) {
			return new VoidWriteResponseEndpoint(delegate);
		}

	}

	@Configuration
	@Import(BaseConfiguration.class)
	static class VoidDeleteResponseEndpointConfiguration {

		@Bean
		public VoidDeleteResponseEndpoint voidDeleteResponseEndpoint(
				EndpointDelegate delegate) {
			return new VoidDeleteResponseEndpoint(delegate);
		}

	}

	@Configuration
	@Import(BaseConfiguration.class)
	static class NullWriteResponseEndpointConfiguration {

		@Bean
		public NullWriteResponseEndpoint nullWriteResponseEndpoint(
				EndpointDelegate delegate) {
			return new NullWriteResponseEndpoint(delegate);
		}

	}

	@Configuration
	@Import(BaseConfiguration.class)
	static class NullReadResponseEndpointConfiguration {

		@Bean
		public NullReadResponseEndpoint nullResponseEndpoint() {
			return new NullReadResponseEndpoint();
		}

	}

	@Configuration
	@Import(BaseConfiguration.class)
	static class NullDeleteResponseEndpointConfiguration {

		@Bean
		public NullDeleteResponseEndpoint nullDeleteResponseEndpoint() {
			return new NullDeleteResponseEndpoint();
		}

	}

	@Configuration
	@Import(BaseConfiguration.class)
	protected static class ResourceEndpointConfiguration {

		@Bean
		public ResourceEndpoint resourceEndpoint() {
			return new ResourceEndpoint();
		}

	}

	@Configuration
	@Import(BaseConfiguration.class)
	static class ResourceWebEndpointResponseEndpointConfiguration {

		@Bean
		public ResourceWebEndpointResponseEndpoint resourceEndpoint() {
			return new ResourceWebEndpointResponseEndpoint();
		}

	}

	@Configuration
	@Import(BaseConfiguration.class)
	static class MonoResponseEndpointConfiguration {

		@Bean
		public MonoResponseEndpoint testEndpoint(EndpointDelegate endpointDelegate) {
			return new MonoResponseEndpoint();
		}

	}

	@Configuration
	@Import(BaseConfiguration.class)
	static class CustomMediaTypesEndpointConfiguration {

		@Bean
		public CustomMediaTypesEndpoint customMediaTypesEndpoint() {
			return new CustomMediaTypesEndpoint();
		}

	}

	@Configuration
	@Import(BaseConfiguration.class)
	static class RequiredParameterEndpointConfiguration {

		@Bean
		public RequiredParametersEndpoint requiredParametersEndpoint() {
			return new RequiredParametersEndpoint();
		}

	}

	@Endpoint(id = "test")
	static class TestEndpoint {

		private final EndpointDelegate endpointDelegate;

		TestEndpoint(EndpointDelegate endpointDelegate) {
			this.endpointDelegate = endpointDelegate;
		}

		@ReadOperation
		public Map<String, Object> readAll() {
			return Collections.singletonMap("All", true);
		}

		@ReadOperation
		public Map<String, Object> readPart(@Selector String part) {
			return Collections.singletonMap("part", part);
		}

		@WriteOperation
		public void write(@Nullable String foo, @Nullable String bar) {
			this.endpointDelegate.write(foo, bar);
		}

		@DeleteOperation
		public Map<String, Object> deletePart(@Selector String part) {
			return Collections.singletonMap("part", part);
		}

	}

	@Endpoint(id = "query")
	static class QueryEndpoint {

		@ReadOperation
		public Map<String, String> query(String one, Integer two) {
			return Collections.singletonMap("query", one + " " + two);
		}

		@ReadOperation
		public Map<String, String> queryWithParameterList(@Selector String list,
				String one, List<String> two) {
			return Collections.singletonMap("query", list + " " + one + " " + two);
		}

	}

	@Endpoint(id = "query")
	static class QueryWithListEndpoint {

		@ReadOperation
		public Map<String, String> queryWithParameterList(String one, List<String> two) {
			return Collections.singletonMap("query", one + " " + two);
		}

	}

	@Endpoint(id = "voidwrite")
	static class VoidWriteResponseEndpoint {

		private final EndpointDelegate delegate;

		VoidWriteResponseEndpoint(EndpointDelegate delegate) {
			this.delegate = delegate;
		}

		@WriteOperation
		public void write() {
			this.delegate.write();
		}

	}

	@Endpoint(id = "voiddelete")
	static class VoidDeleteResponseEndpoint {

		private final EndpointDelegate delegate;

		VoidDeleteResponseEndpoint(EndpointDelegate delegate) {
			this.delegate = delegate;
		}

		@DeleteOperation
		public void delete() {
			this.delegate.delete();
		}

	}

	@Endpoint(id = "nullwrite")
	static class NullWriteResponseEndpoint {

		private final EndpointDelegate delegate;

		NullWriteResponseEndpoint(EndpointDelegate delegate) {
			this.delegate = delegate;
		}

		@WriteOperation
		public Object write() {
			this.delegate.write();
			return null;
		}

	}

	@Endpoint(id = "nullread")
	static class NullReadResponseEndpoint {

		@ReadOperation
		public String readReturningNull() {
			return null;
		}

	}

	@Endpoint(id = "nulldelete")
	static class NullDeleteResponseEndpoint {

		@DeleteOperation
		public String deleteReturningNull() {
			return null;
		}

	}

	@Endpoint(id = "resource")
	static class ResourceEndpoint {

		@ReadOperation
		public Resource read() {
			return new ByteArrayResource(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 });
		}

	}

	@Endpoint(id = "resource")
	static class ResourceWebEndpointResponseEndpoint {

		@ReadOperation
		public WebEndpointResponse<Resource> read() {
			return new WebEndpointResponse<>(
					new ByteArrayResource(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 }),
					200);
		}

	}

	@Endpoint(id = "mono")
	static class MonoResponseEndpoint {

		@ReadOperation
		Mono<Map<String, String>> operation() {
			return Mono.just(Collections.singletonMap("a", "alpha"));
		}

	}

	@Endpoint(id = "custommediatypes")
	static class CustomMediaTypesEndpoint {

		@ReadOperation(produces = "text/plain")
		public String read() {
			return "read";
		}

	}

	@Endpoint(id = "requiredparameters")
	static class RequiredParametersEndpoint {

		@ReadOperation
		public String read(String foo, @Nullable String bar) {
			return foo;
		}

	}

	public interface EndpointDelegate {

		void write();

		void write(String foo, String bar);

		void delete();

	}

}
