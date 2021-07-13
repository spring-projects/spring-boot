/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.web.annotation;

import java.net.InetSocketAddress;
import java.security.Principal;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.endpoint.annotation.DeleteOperation;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.annotation.Selector.Match;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * Abstract base class for web endpoint integration tests.
 *
 * @param <T> the type of application context used by the tests
 * @author Andy Wilkinson
 * @author Scott Frederick
 */
public abstract class AbstractWebEndpointIntegrationTests<T extends ConfigurableApplicationContext & AnnotationConfigRegistry> {

	private static final Duration TIMEOUT = Duration.ofMinutes(5);

	private static final String ACTUATOR_MEDIA_TYPE_PATTERN = "application/vnd.test\\+json(;charset=UTF-8)?";

	private static final String JSON_MEDIA_TYPE_PATTERN = "application/json(;charset=UTF-8)?";

	private final Supplier<T> applicationContextSupplier;

	private final Consumer<T> authenticatedContextCustomizer;

	protected AbstractWebEndpointIntegrationTests(Supplier<T> applicationContextSupplier,
			Consumer<T> authenticatedContextCustomizer) {
		this.applicationContextSupplier = applicationContextSupplier;
		this.authenticatedContextCustomizer = authenticatedContextCustomizer;
	}

	@Test
	void readOperation() {
		load(TestEndpointConfiguration.class, (client) -> client.get().uri("/test").exchange().expectStatus().isOk()
				.expectBody().jsonPath("All").isEqualTo(true));
	}

	@Test
	void readOperationWithEndpointsMappedToTheRoot() {
		load(TestEndpointConfiguration.class, "", (client) -> client.get().uri("/test").exchange().expectStatus().isOk()
				.expectBody().jsonPath("All").isEqualTo(true));
	}

	@Test
	void readOperationWithSelector() {
		load(TestEndpointConfiguration.class, (client) -> client.get().uri("/test/one").exchange().expectStatus().isOk()
				.expectBody().jsonPath("part").isEqualTo("one"));
	}

	@Test
	void readOperationWithSelectorContainingADot() {
		load(TestEndpointConfiguration.class, (client) -> client.get().uri("/test/foo.bar").exchange().expectStatus()
				.isOk().expectBody().jsonPath("part").isEqualTo("foo.bar"));
	}

	@Test
	void linksToOtherEndpointsAreProvided() {
		load(TestEndpointConfiguration.class,
				(client) -> client.get().uri("").exchange().expectStatus().isOk().expectBody()
						.jsonPath("_links.length()").isEqualTo(3).jsonPath("_links.self.href").isNotEmpty()
						.jsonPath("_links.self.templated").isEqualTo(false).jsonPath("_links.test.href").isNotEmpty()
						.jsonPath("_links.test.templated").isEqualTo(false).jsonPath("_links.test-part.href")
						.isNotEmpty().jsonPath("_links.test-part.templated").isEqualTo(true));
	}

	@Test
	void linksMappingIsDisabledWhenEndpointPathIsEmpty() {
		load(TestEndpointConfiguration.class, "",
				(client) -> client.get().uri("").exchange().expectStatus().isNotFound());
	}

	@Test
	void operationWithTrailingSlashShouldMatch() {
		load(TestEndpointConfiguration.class, (client) -> client.get().uri("/test/").exchange().expectStatus().isOk()
				.expectBody().jsonPath("All").isEqualTo(true));
	}

	@Test
	void matchAllRemainingPathsSelectorShouldMatchFullPath() {
		load(MatchAllRemainingEndpointConfiguration.class,
				(client) -> client.get().uri("/matchallremaining/one/two/three").exchange().expectStatus().isOk()
						.expectBody().jsonPath("selection").isEqualTo("one|two|three"));
	}

	@Test
	void matchAllRemainingPathsSelectorShouldDecodePath() {
		load(MatchAllRemainingEndpointConfiguration.class,
				(client) -> client.get().uri("/matchallremaining/one/two three/").exchange().expectStatus().isOk()
						.expectBody().jsonPath("selection").isEqualTo("one|two three"));
	}

	@Test
	void readOperationWithSingleQueryParameters() {
		load(QueryEndpointConfiguration.class, (client) -> client.get().uri("/query?one=1&two=2").exchange()
				.expectStatus().isOk().expectBody().jsonPath("query").isEqualTo("1 2"));
	}

	@Test
	void readOperationWithSingleQueryParametersAndMultipleValues() {
		load(QueryEndpointConfiguration.class, (client) -> client.get().uri("/query?one=1&one=1&two=2").exchange()
				.expectStatus().isOk().expectBody().jsonPath("query").isEqualTo("1,1 2"));
	}

	@Test
	void readOperationWithListQueryParameterAndSingleValue() {
		load(QueryWithListEndpointConfiguration.class, (client) -> client.get().uri("/query?one=1&two=2").exchange()
				.expectStatus().isOk().expectBody().jsonPath("query").isEqualTo("1 [2]"));
	}

	@Test
	void readOperationWithListQueryParameterAndMultipleValues() {
		load(QueryWithListEndpointConfiguration.class, (client) -> client.get().uri("/query?one=1&two=2&two=2")
				.exchange().expectStatus().isOk().expectBody().jsonPath("query").isEqualTo("1 [2, 2]"));
	}

	@Test
	void readOperationWithMappingFailureProducesBadRequestResponse() {
		load(QueryEndpointConfiguration.class, (client) -> {
			WebTestClient.BodyContentSpec body = client.get().uri("/query?two=two").accept(MediaType.APPLICATION_JSON)
					.exchange().expectStatus().isBadRequest().expectBody();
			validateErrorBody(body, HttpStatus.BAD_REQUEST, "/endpoints/query", "Missing parameters: one");
		});
	}

	@Test
	void writeOperation() {
		load(TestEndpointConfiguration.class, (client) -> {
			Map<String, Object> body = new HashMap<>();
			body.put("foo", "one");
			body.put("bar", "two");
			client.post().uri("/test").bodyValue(body).exchange().expectStatus().isNoContent().expectBody().isEmpty();
		});
	}

	@Test
	void writeOperationWithVoidResponse() {
		load(VoidWriteResponseEndpointConfiguration.class, (context, client) -> {
			client.post().uri("/voidwrite").exchange().expectStatus().isNoContent().expectBody().isEmpty();
			verify(context.getBean(EndpointDelegate.class)).write();
		});
	}

	@Test
	void deleteOperation() {
		load(TestEndpointConfiguration.class, (client) -> client.delete().uri("/test/one").exchange().expectStatus()
				.isOk().expectBody().jsonPath("part").isEqualTo("one"));
	}

	@Test
	void deleteOperationWithVoidResponse() {
		load(VoidDeleteResponseEndpointConfiguration.class, (context, client) -> {
			client.delete().uri("/voiddelete").exchange().expectStatus().isNoContent().expectBody().isEmpty();
			verify(context.getBean(EndpointDelegate.class)).delete();
		});
	}

	@Test
	void nullIsPassedToTheOperationWhenArgumentIsNotFoundInPostRequestBody() {
		load(TestEndpointConfiguration.class, (context, client) -> {
			Map<String, Object> body = new HashMap<>();
			body.put("foo", "one");
			client.post().uri("/test").bodyValue(body).exchange().expectStatus().isNoContent().expectBody().isEmpty();
			verify(context.getBean(EndpointDelegate.class)).write("one", null);
		});
	}

	@Test
	void nullsArePassedToTheOperationWhenPostRequestHasNoBody() {
		load(TestEndpointConfiguration.class, (context, client) -> {
			client.post().uri("/test").contentType(MediaType.APPLICATION_JSON).exchange().expectStatus().isNoContent()
					.expectBody().isEmpty();
			verify(context.getBean(EndpointDelegate.class)).write(null, null);
		});
	}

	@Test
	void nullResponseFromReadOperationResultsInNotFoundResponseStatus() {
		load(NullReadResponseEndpointConfiguration.class,
				(context, client) -> client.get().uri("/nullread").exchange().expectStatus().isNotFound());
	}

	@Test
	void nullResponseFromDeleteOperationResultsInNoContentResponseStatus() {
		load(NullDeleteResponseEndpointConfiguration.class,
				(context, client) -> client.delete().uri("/nulldelete").exchange().expectStatus().isNoContent());
	}

	@Test
	void nullResponseFromWriteOperationResultsInNoContentResponseStatus() {
		load(NullWriteResponseEndpointConfiguration.class,
				(context, client) -> client.post().uri("/nullwrite").exchange().expectStatus().isNoContent());
	}

	@Test
	void readOperationWithResourceResponse() {
		load(ResourceEndpointConfiguration.class, (context, client) -> {
			byte[] responseBody = client.get().uri("/resource").exchange().expectStatus().isOk().expectHeader()
					.contentType(MediaType.APPLICATION_OCTET_STREAM).returnResult(byte[].class)
					.getResponseBodyContent();
			assertThat(responseBody).containsExactly(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
		});
	}

	@Test
	void readOperationWithResourceWebOperationResponse() {
		load(ResourceWebEndpointResponseEndpointConfiguration.class, (context, client) -> {
			byte[] responseBody = client.get().uri("/resource").exchange().expectStatus().isOk().expectHeader()
					.contentType(MediaType.APPLICATION_OCTET_STREAM).returnResult(byte[].class)
					.getResponseBodyContent();
			assertThat(responseBody).containsExactly(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
		});
	}

	@Test
	void readOperationWithMonoResponse() {
		load(MonoResponseEndpointConfiguration.class, (client) -> client.get().uri("/mono").exchange().expectStatus()
				.isOk().expectBody().jsonPath("a").isEqualTo("alpha"));
	}

	@Test
	void readOperationWithCustomMediaType() {
		load(CustomMediaTypesEndpointConfiguration.class, (client) -> client.get().uri("/custommediatypes").exchange()
				.expectStatus().isOk().expectHeader().valueMatches("Content-Type", "text/plain(;charset=.*)?"));
	}

	@Test
	void readOperationWithMissingRequiredParametersReturnsBadRequestResponse() {
		load(RequiredParameterEndpointConfiguration.class, (client) -> {
			WebTestClient.BodyContentSpec body = client.get().uri("/requiredparameters")
					.accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isBadRequest().expectBody();
			validateErrorBody(body, HttpStatus.BAD_REQUEST, "/endpoints/requiredparameters", "Missing parameters: foo");
		});
	}

	@Test
	void readOperationWithMissingNullableParametersIsOk() {
		load(RequiredParameterEndpointConfiguration.class,
				(client) -> client.get().uri("/requiredparameters?foo=hello").exchange().expectStatus().isOk());
	}

	@Test
	void endpointsProducePrimaryMediaTypeByDefault() {
		load(TestEndpointConfiguration.class, (client) -> client.get().uri("/test").exchange().expectStatus().isOk()
				.expectHeader().valueMatches("Content-Type", ACTUATOR_MEDIA_TYPE_PATTERN));
	}

	@Test
	void endpointsProduceSecondaryMediaTypeWhenRequested() {
		load(TestEndpointConfiguration.class, (client) -> client.get().uri("/test").accept(MediaType.APPLICATION_JSON)
				.exchange().expectStatus().isOk().expectHeader().valueMatches("Content-Type", JSON_MEDIA_TYPE_PATTERN));
	}

	@Test
	void linksProducesPrimaryMediaTypeByDefault() {
		load(TestEndpointConfiguration.class, (client) -> client.get().uri("").exchange().expectStatus().isOk()
				.expectHeader().valueMatches("Content-Type", ACTUATOR_MEDIA_TYPE_PATTERN));
	}

	@Test
	void linksProducesSecondaryMediaTypeWhenRequested() {
		load(TestEndpointConfiguration.class, (client) -> client.get().uri("").accept(MediaType.APPLICATION_JSON)
				.exchange().expectStatus().isOk().expectHeader().valueMatches("Content-Type", JSON_MEDIA_TYPE_PATTERN));
	}

	@Test
	void principalIsNullWhenRequestHasNoPrincipal() {
		load(PrincipalEndpointConfiguration.class,
				(client) -> client.get().uri("/principal").accept(MediaType.APPLICATION_JSON).exchange().expectStatus()
						.isOk().expectBody(String.class).isEqualTo("None"));
	}

	@Test
	void principalIsAvailableWhenRequestHasAPrincipal() {
		load((context) -> {
			this.authenticatedContextCustomizer.accept(context);
			context.register(PrincipalEndpointConfiguration.class);
		}, (client) -> client.get().uri("/principal").accept(MediaType.APPLICATION_JSON).exchange().expectStatus()
				.isOk().expectBody(String.class).isEqualTo("Alice"));
	}

	@Test
	void operationWithAQueryNamedPrincipalCanBeAccessedWhenAuthenticated() {
		load((context) -> {
			this.authenticatedContextCustomizer.accept(context);
			context.register(PrincipalQueryEndpointConfiguration.class);
		}, (client) -> client.get().uri("/principalquery?principal=Zoe").accept(MediaType.APPLICATION_JSON).exchange()
				.expectStatus().isOk().expectBody(String.class).isEqualTo("Zoe"));
	}

	@Test
	void securityContextIsAvailableAndHasNullPrincipalWhenRequestHasNoPrincipal() {
		load(SecurityContextEndpointConfiguration.class,
				(client) -> client.get().uri("/securitycontext").accept(MediaType.APPLICATION_JSON).exchange()
						.expectStatus().isOk().expectBody(String.class).isEqualTo("None"));
	}

	@Test
	void securityContextIsAvailableAndHasPrincipalWhenRequestHasPrincipal() {
		load((context) -> {
			this.authenticatedContextCustomizer.accept(context);
			context.register(SecurityContextEndpointConfiguration.class);
		}, (client) -> client.get().uri("/securitycontext").accept(MediaType.APPLICATION_JSON).exchange().expectStatus()
				.isOk().expectBody(String.class).isEqualTo("Alice"));
	}

	@Test
	void userInRoleReturnsFalseWhenRequestHasNoPrincipal() {
		load(UserInRoleEndpointConfiguration.class,
				(client) -> client.get().uri("/userinrole?role=ADMIN").accept(MediaType.APPLICATION_JSON).exchange()
						.expectStatus().isOk().expectBody(String.class).isEqualTo("ADMIN: false"));
	}

	@Test
	void userInRoleReturnsFalseWhenUserIsNotInRole() {
		load((context) -> {
			this.authenticatedContextCustomizer.accept(context);
			context.register(UserInRoleEndpointConfiguration.class);
		}, (client) -> client.get().uri("/userinrole?role=ADMIN").accept(MediaType.APPLICATION_JSON).exchange()
				.expectStatus().isOk().expectBody(String.class).isEqualTo("ADMIN: false"));
	}

	@Test
	void userInRoleReturnsTrueWhenUserIsInRole() {
		load((context) -> {
			this.authenticatedContextCustomizer.accept(context);
			context.register(UserInRoleEndpointConfiguration.class);
		}, (client) -> client.get().uri("/userinrole?role=ACTUATOR").accept(MediaType.APPLICATION_JSON).exchange()
				.expectStatus().isOk().expectBody(String.class).isEqualTo("ACTUATOR: true"));
	}

	@Test
	void endpointCanProduceAResponseWithACustomStatus() {
		load((context) -> context.register(CustomResponseStatusEndpointConfiguration.class),
				(client) -> client.get().uri("/customstatus").exchange().expectStatus().isEqualTo(234));
	}

	protected abstract int getPort(T context);

	protected void validateErrorBody(WebTestClient.BodyContentSpec body, HttpStatus status, String path,
			String message) {
		body.jsonPath("status").isEqualTo(status.value()).jsonPath("error").isEqualTo(status.getReasonPhrase())
				.jsonPath("path").isEqualTo(path).jsonPath("message").isEqualTo(message);
	}

	private void load(Class<?> configuration, BiConsumer<ApplicationContext, WebTestClient> consumer) {
		load((context) -> context.register(configuration), "/endpoints", consumer);
	}

	protected void load(Class<?> configuration, Consumer<WebTestClient> clientConsumer) {
		load((context) -> context.register(configuration), "/endpoints",
				(context, client) -> clientConsumer.accept(client));
	}

	protected void load(Consumer<T> contextCustomizer, Consumer<WebTestClient> clientConsumer) {
		load(contextCustomizer, "/endpoints", (context, client) -> clientConsumer.accept(client));
	}

	protected void load(Class<?> configuration, String endpointPath, Consumer<WebTestClient> clientConsumer) {
		load((context) -> context.register(configuration), endpointPath,
				(context, client) -> clientConsumer.accept(client));
	}

	private void load(Consumer<T> contextCustomizer, String endpointPath,
			BiConsumer<ApplicationContext, WebTestClient> consumer) {
		T applicationContext = this.applicationContextSupplier.get();
		contextCustomizer.accept(applicationContext);
		Map<String, Object> properties = new HashMap<>();
		properties.put("endpointPath", endpointPath);
		properties.put("server.error.include-message", "always");
		applicationContext.getEnvironment().getPropertySources().addLast(new MapPropertySource("test", properties));
		applicationContext.refresh();
		try {
			InetSocketAddress address = new InetSocketAddress(getPort(applicationContext));
			String url = "http://" + address.getHostString() + ":" + address.getPort() + endpointPath;
			consumer.accept(applicationContext,
					WebTestClient.bindToServer().baseUrl(url).responseTimeout(TIMEOUT).build());
		}
		finally {
			applicationContext.close();
		}
	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseConfiguration.class)
	protected static class TestEndpointConfiguration {

		@Bean
		public TestEndpoint testEndpoint(EndpointDelegate endpointDelegate) {
			return new TestEndpoint(endpointDelegate);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseConfiguration.class)
	static class MatchAllRemainingEndpointConfiguration {

		@Bean
		MatchAllRemainingEndpoint matchAllRemainingEndpoint() {
			return new MatchAllRemainingEndpoint();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseConfiguration.class)
	static class QueryEndpointConfiguration {

		@Bean
		QueryEndpoint queryEndpoint() {
			return new QueryEndpoint();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseConfiguration.class)
	static class QueryWithListEndpointConfiguration {

		@Bean
		QueryWithListEndpoint queryEndpoint() {
			return new QueryWithListEndpoint();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseConfiguration.class)
	static class VoidWriteResponseEndpointConfiguration {

		@Bean
		VoidWriteResponseEndpoint voidWriteResponseEndpoint(EndpointDelegate delegate) {
			return new VoidWriteResponseEndpoint(delegate);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseConfiguration.class)
	static class VoidDeleteResponseEndpointConfiguration {

		@Bean
		VoidDeleteResponseEndpoint voidDeleteResponseEndpoint(EndpointDelegate delegate) {
			return new VoidDeleteResponseEndpoint(delegate);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseConfiguration.class)
	static class NullWriteResponseEndpointConfiguration {

		@Bean
		NullWriteResponseEndpoint nullWriteResponseEndpoint(EndpointDelegate delegate) {
			return new NullWriteResponseEndpoint(delegate);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseConfiguration.class)
	static class NullReadResponseEndpointConfiguration {

		@Bean
		NullReadResponseEndpoint nullResponseEndpoint() {
			return new NullReadResponseEndpoint();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseConfiguration.class)
	static class NullDeleteResponseEndpointConfiguration {

		@Bean
		NullDeleteResponseEndpoint nullDeleteResponseEndpoint() {
			return new NullDeleteResponseEndpoint();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseConfiguration.class)
	protected static class ResourceEndpointConfiguration {

		@Bean
		public ResourceEndpoint resourceEndpoint() {
			return new ResourceEndpoint();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseConfiguration.class)
	static class ResourceWebEndpointResponseEndpointConfiguration {

		@Bean
		ResourceWebEndpointResponseEndpoint resourceEndpoint() {
			return new ResourceWebEndpointResponseEndpoint();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseConfiguration.class)
	static class MonoResponseEndpointConfiguration {

		@Bean
		MonoResponseEndpoint testEndpoint(EndpointDelegate endpointDelegate) {
			return new MonoResponseEndpoint();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseConfiguration.class)
	static class CustomMediaTypesEndpointConfiguration {

		@Bean
		CustomMediaTypesEndpoint customMediaTypesEndpoint() {
			return new CustomMediaTypesEndpoint();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseConfiguration.class)
	static class RequiredParameterEndpointConfiguration {

		@Bean
		RequiredParametersEndpoint requiredParametersEndpoint() {
			return new RequiredParametersEndpoint();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseConfiguration.class)
	static class PrincipalEndpointConfiguration {

		@Bean
		PrincipalEndpoint principalEndpoint() {
			return new PrincipalEndpoint();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseConfiguration.class)
	static class PrincipalQueryEndpointConfiguration {

		@Bean
		PrincipalQueryEndpoint principalQueryEndpoint() {
			return new PrincipalQueryEndpoint();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseConfiguration.class)
	static class SecurityContextEndpointConfiguration {

		@Bean
		SecurityContextEndpoint securityContextEndpoint() {
			return new SecurityContextEndpoint();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseConfiguration.class)
	static class UserInRoleEndpointConfiguration {

		@Bean
		UserInRoleEndpoint userInRoleEndpoint() {
			return new UserInRoleEndpoint();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseConfiguration.class)
	static class CustomResponseStatusEndpointConfiguration {

		@Bean
		CustomResponseStatusEndpoint customResponseStatusEndpoint() {
			return new CustomResponseStatusEndpoint();
		}

	}

	@Endpoint(id = "test")
	static class TestEndpoint {

		private final EndpointDelegate endpointDelegate;

		TestEndpoint(EndpointDelegate endpointDelegate) {
			this.endpointDelegate = endpointDelegate;
		}

		@ReadOperation
		Map<String, Object> readAll() {
			return Collections.singletonMap("All", true);
		}

		@ReadOperation
		Map<String, Object> readPart(@Selector String part) {
			return Collections.singletonMap("part", part);
		}

		@WriteOperation
		void write(@Nullable String foo, @Nullable String bar) {
			this.endpointDelegate.write(foo, bar);
		}

		@DeleteOperation
		Map<String, Object> deletePart(@Selector String part) {
			return Collections.singletonMap("part", part);
		}

	}

	@Endpoint(id = "matchallremaining")
	static class MatchAllRemainingEndpoint {

		@ReadOperation
		Map<String, String> select(@Selector(match = Match.ALL_REMAINING) String... selection) {
			return Collections.singletonMap("selection", StringUtils.arrayToDelimitedString(selection, "|"));
		}

	}

	@Endpoint(id = "query")
	static class QueryEndpoint {

		@ReadOperation
		Map<String, String> query(String one, Integer two) {
			return Collections.singletonMap("query", one + " " + two);
		}

		@ReadOperation
		Map<String, String> queryWithParameterList(@Selector String list, String one, List<String> two) {
			return Collections.singletonMap("query", list + " " + one + " " + two);
		}

	}

	@Endpoint(id = "query")
	static class QueryWithListEndpoint {

		@ReadOperation
		Map<String, String> queryWithParameterList(String one, List<String> two) {
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
		void write() {
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
		void delete() {
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
		Object write() {
			this.delegate.write();
			return null;
		}

	}

	@Endpoint(id = "nullread")
	static class NullReadResponseEndpoint {

		@ReadOperation
		String readReturningNull() {
			return null;
		}

	}

	@Endpoint(id = "nulldelete")
	static class NullDeleteResponseEndpoint {

		@DeleteOperation
		String deleteReturningNull() {
			return null;
		}

	}

	@Endpoint(id = "resource")
	static class ResourceEndpoint {

		@ReadOperation
		Resource read() {
			return new ByteArrayResource(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 });
		}

	}

	@Endpoint(id = "resource")
	static class ResourceWebEndpointResponseEndpoint {

		@ReadOperation
		WebEndpointResponse<Resource> read() {
			return new WebEndpointResponse<>(new ByteArrayResource(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 }), 200);
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
		String read() {
			return "read";
		}

	}

	@Endpoint(id = "requiredparameters")
	static class RequiredParametersEndpoint {

		@ReadOperation
		String read(String foo, @Nullable String bar) {
			return foo;
		}

	}

	@Endpoint(id = "principal")
	static class PrincipalEndpoint {

		@ReadOperation
		String read(@Nullable Principal principal) {
			return (principal != null) ? principal.getName() : "None";
		}

	}

	@Endpoint(id = "principalquery")
	static class PrincipalQueryEndpoint {

		@ReadOperation
		String read(String principal) {
			return principal;
		}

	}

	@Endpoint(id = "securitycontext")
	static class SecurityContextEndpoint {

		@ReadOperation
		String read(SecurityContext securityContext) {
			Principal principal = securityContext.getPrincipal();
			return (principal != null) ? principal.getName() : "None";
		}

	}

	@Endpoint(id = "userinrole")
	static class UserInRoleEndpoint {

		@ReadOperation
		String read(SecurityContext securityContext, String role) {
			return role + ": " + securityContext.isUserInRole(role);
		}

	}

	@Endpoint(id = "customstatus")
	static class CustomResponseStatusEndpoint {

		@ReadOperation
		WebEndpointResponse<String> read() {
			return new WebEndpointResponse<>("Custom status", 234);
		}

	}

	interface EndpointDelegate {

		void write();

		void write(String foo, String bar);

		void delete();

	}

}
