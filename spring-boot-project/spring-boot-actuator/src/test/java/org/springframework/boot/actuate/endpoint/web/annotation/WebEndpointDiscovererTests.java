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

package org.springframework.boot.actuate.endpoint.web.annotation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.annotation.DeleteOperation;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.boot.actuate.endpoint.invoke.convert.ConversionServiceParameterValueMapper;
import org.springframework.boot.actuate.endpoint.invoker.cache.CachingOperationInvoker;
import org.springframework.boot.actuate.endpoint.invoker.cache.CachingOperationInvokerAdvisor;
import org.springframework.boot.actuate.endpoint.jmx.annotation.JmxEndpoint;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.boot.actuate.endpoint.web.PathMapper;
import org.springframework.boot.actuate.endpoint.web.WebEndpointHttpMethod;
import org.springframework.boot.actuate.endpoint.web.WebOperation;
import org.springframework.boot.actuate.endpoint.web.WebOperationRequestPredicate;
import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpointDiscoverer.WebEndpointDiscovererRuntimeHints;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link WebEndpointDiscoverer}.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Moritz Halbritter
 */
class WebEndpointDiscovererTests {

	@Test
	void getEndpointsWhenNoEndpointBeansShouldReturnEmptyCollection() {
		load(EmptyConfiguration.class, (discoverer) -> assertThat(discoverer.getEndpoints()).isEmpty());
	}

	@Test
	void getEndpointsWhenWebExtensionIsMissingEndpointShouldThrowException() {
		load(TestWebEndpointExtensionConfiguration.class,
				(discoverer) -> assertThatIllegalStateException().isThrownBy(discoverer::getEndpoints)
					.withMessageContaining("Invalid extension 'endpointExtension': no endpoint found with id 'test'"));
	}

	@Test
	void getEndpointsWhenHasFilteredEndpointShouldOnlyDiscoverWebEndpoints() {
		load(MultipleEndpointsConfiguration.class, (discoverer) -> {
			Map<EndpointId, ExposableWebEndpoint> endpoints = mapEndpoints(discoverer.getEndpoints());
			assertThat(endpoints).containsOnlyKeys(EndpointId.of("test"));
		});
	}

	@Test
	void getEndpointsWhenHasWebExtensionShouldOverrideStandardEndpoint() {
		load(OverriddenOperationWebEndpointExtensionConfiguration.class, (discoverer) -> {
			Map<EndpointId, ExposableWebEndpoint> endpoints = mapEndpoints(discoverer.getEndpoints());
			assertThat(endpoints).containsOnlyKeys(EndpointId.of("test"));
			ExposableWebEndpoint endpoint = endpoints.get(EndpointId.of("test"));
			assertThat(requestPredicates(endpoint)).has(requestPredicates(
					path("test").httpMethod(WebEndpointHttpMethod.GET).consumes().produces("application/json")));
		});
	}

	@Test
	void getEndpointsWhenExtensionAddsOperationShouldHaveBothOperations() {
		load(AdditionalOperationWebEndpointConfiguration.class, (discoverer) -> {
			Map<EndpointId, ExposableWebEndpoint> endpoints = mapEndpoints(discoverer.getEndpoints());
			assertThat(endpoints).containsOnlyKeys(EndpointId.of("test"));
			ExposableWebEndpoint endpoint = endpoints.get(EndpointId.of("test"));
			assertThat(requestPredicates(endpoint)).has(requestPredicates(
					path("test").httpMethod(WebEndpointHttpMethod.GET).consumes().produces("application/json"),
					path("test/{id}").httpMethod(WebEndpointHttpMethod.GET).consumes().produces("application/json")));
		});
	}

	@Test
	void getEndpointsWhenPredicateForWriteOperationThatReturnsVoidShouldHaveNoProducedMediaTypes() {
		load(VoidWriteOperationEndpointConfiguration.class, (discoverer) -> {
			Map<EndpointId, ExposableWebEndpoint> endpoints = mapEndpoints(discoverer.getEndpoints());
			assertThat(endpoints).containsOnlyKeys(EndpointId.of("voidwrite"));
			ExposableWebEndpoint endpoint = endpoints.get(EndpointId.of("voidwrite"));
			assertThat(requestPredicates(endpoint)).has(requestPredicates(
					path("voidwrite").httpMethod(WebEndpointHttpMethod.POST).produces().consumes("application/json")));
		});
	}

	@Test
	void getEndpointsWhenTwoExtensionsHaveTheSameEndpointTypeShouldThrowException() {
		load(ClashingWebEndpointConfiguration.class,
				(discoverer) -> assertThatIllegalStateException().isThrownBy(discoverer::getEndpoints)
					.withMessageContaining("Found multiple extensions for the endpoint bean "
							+ "testEndpoint (testExtensionOne, testExtensionTwo)"));
	}

	@Test
	void getEndpointsWhenTwoStandardEndpointsHaveTheSameIdShouldThrowException() {
		load(ClashingStandardEndpointConfiguration.class,
				(discoverer) -> assertThatIllegalStateException().isThrownBy(discoverer::getEndpoints)
					.withMessageContaining("Found two endpoints with the id 'test': "));
	}

	@Test
	void getEndpointsWhenWhenEndpointHasTwoOperationsWithTheSameNameShouldThrowException() {
		load(ClashingOperationsEndpointConfiguration.class,
				(discoverer) -> assertThatIllegalStateException().isThrownBy(discoverer::getEndpoints)
					.withMessageContaining("Unable to map duplicate endpoint operations: "
							+ "[web request predicate GET to path 'test' "
							+ "produces: application/json] to clashingOperationsEndpoint"));
	}

	@Test
	void getEndpointsWhenExtensionIsNotCompatibleWithTheEndpointTypeShouldThrowException() {
		load(InvalidWebExtensionConfiguration.class,
				(discoverer) -> assertThatIllegalStateException().isThrownBy(discoverer::getEndpoints)
					.withMessageContaining("Endpoint bean 'nonWebEndpoint' cannot support the "
							+ "extension bean 'nonWebWebEndpointExtension'"));
	}

	@Test
	void getEndpointsWhenWhenExtensionHasTwoOperationsWithTheSameNameShouldThrowException() {
		load(ClashingSelectorsWebEndpointExtensionConfiguration.class,
				(discoverer) -> assertThatIllegalStateException().isThrownBy(discoverer::getEndpoints)
					.withMessageContaining("Unable to map duplicate endpoint operations")
					.withMessageContaining("to testEndpoint (clashingSelectorsExtension)"));
	}

	@Test
	void getEndpointsWhenHasCacheWithTtlShouldCacheReadOperationWithTtlValue() {
		load((id) -> 500L, EndpointId::toString, TestEndpointConfiguration.class, (discoverer) -> {
			Map<EndpointId, ExposableWebEndpoint> endpoints = mapEndpoints(discoverer.getEndpoints());
			assertThat(endpoints).containsOnlyKeys(EndpointId.of("test"));
			ExposableWebEndpoint endpoint = endpoints.get(EndpointId.of("test"));
			assertThat(endpoint.getOperations()).hasSize(1);
			WebOperation operation = endpoint.getOperations().iterator().next();
			Object invoker = ReflectionTestUtils.getField(operation, "invoker");
			assertThat(invoker).isInstanceOf(CachingOperationInvoker.class);
			assertThat(((CachingOperationInvoker) invoker).getTimeToLive()).isEqualTo(500);
		});
	}

	@Test
	void getEndpointsWhenOperationReturnsResourceShouldProduceApplicationOctetStream() {
		load(ResourceEndpointConfiguration.class, (discoverer) -> {
			Map<EndpointId, ExposableWebEndpoint> endpoints = mapEndpoints(discoverer.getEndpoints());
			assertThat(endpoints).containsOnlyKeys(EndpointId.of("resource"));
			ExposableWebEndpoint endpoint = endpoints.get(EndpointId.of("resource"));
			assertThat(requestPredicates(endpoint))
				.has(requestPredicates(path("resource").httpMethod(WebEndpointHttpMethod.GET)
					.consumes()
					.produces("application/octet-stream")));
		});
	}

	@Test
	void getEndpointsWhenHasCustomMediaTypeShouldProduceCustomMediaType() {
		load(CustomMediaTypesEndpointConfiguration.class, (discoverer) -> {
			Map<EndpointId, ExposableWebEndpoint> endpoints = mapEndpoints(discoverer.getEndpoints());
			assertThat(endpoints).containsOnlyKeys(EndpointId.of("custommediatypes"));
			ExposableWebEndpoint endpoint = endpoints.get(EndpointId.of("custommediatypes"));
			assertThat(requestPredicates(endpoint)).has(requestPredicates(
					path("custommediatypes").httpMethod(WebEndpointHttpMethod.GET).consumes().produces("text/plain"),
					path("custommediatypes").httpMethod(WebEndpointHttpMethod.POST).consumes().produces("a/b", "c/d"),
					path("custommediatypes").httpMethod(WebEndpointHttpMethod.DELETE)
						.consumes()
						.produces("text/plain")));
		});
	}

	@Test
	void getEndpointsWhenHasCustomPathShouldReturnCustomPath() {
		load((id) -> null, (id) -> "custom/" + id, AdditionalOperationWebEndpointConfiguration.class, (discoverer) -> {
			Map<EndpointId, ExposableWebEndpoint> endpoints = mapEndpoints(discoverer.getEndpoints());
			assertThat(endpoints).containsOnlyKeys(EndpointId.of("test"));
			ExposableWebEndpoint endpoint = endpoints.get(EndpointId.of("test"));
			Condition<List<? extends WebOperationRequestPredicate>> expected = requestPredicates(
					path("custom/test").httpMethod(WebEndpointHttpMethod.GET).consumes().produces("application/json"),
					path("custom/test/{id}").httpMethod(WebEndpointHttpMethod.GET)
						.consumes()
						.produces("application/json"));
			assertThat(requestPredicates(endpoint)).has(expected);
		});
	}

	@Test
	void shouldRegisterHints() {
		RuntimeHints runtimeHints = new RuntimeHints();
		new WebEndpointDiscovererRuntimeHints().registerHints(runtimeHints, getClass().getClassLoader());
		assertThat(RuntimeHintsPredicates.reflection()
			.onType(WebEndpointFilter.class)
			.withMemberCategories(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)).accepts(runtimeHints);

	}

	private void load(Class<?> configuration, Consumer<WebEndpointDiscoverer> consumer) {
		load((id) -> null, EndpointId::toString, configuration, consumer);
	}

	private void load(Function<EndpointId, Long> timeToLive, PathMapper endpointPathMapper, Class<?> configuration,
			Consumer<WebEndpointDiscoverer> consumer) {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(configuration)) {
			ConversionServiceParameterValueMapper parameterMapper = new ConversionServiceParameterValueMapper(
					DefaultConversionService.getSharedInstance());
			EndpointMediaTypes mediaTypes = new EndpointMediaTypes(Collections.singletonList("application/json"),
					Collections.singletonList("application/json"));
			WebEndpointDiscoverer discoverer = new WebEndpointDiscoverer(context, parameterMapper, mediaTypes,
					Collections.singletonList(endpointPathMapper),
					Collections.singleton(new CachingOperationInvokerAdvisor(timeToLive)), Collections.emptyList());
			consumer.accept(discoverer);
		}
	}

	private Map<EndpointId, ExposableWebEndpoint> mapEndpoints(Collection<ExposableWebEndpoint> endpoints) {
		Map<EndpointId, ExposableWebEndpoint> endpointById = new HashMap<>();
		endpoints.forEach((endpoint) -> endpointById.put(endpoint.getEndpointId(), endpoint));
		return endpointById;
	}

	private List<WebOperationRequestPredicate> requestPredicates(ExposableWebEndpoint endpoint) {
		return endpoint.getOperations().stream().map(WebOperation::getRequestPredicate).toList();
	}

	private Condition<List<? extends WebOperationRequestPredicate>> requestPredicates(
			RequestPredicateMatcher... matchers) {
		return new Condition<>((predicates) -> {
			if (predicates.size() != matchers.length) {
				return false;
			}
			Map<WebOperationRequestPredicate, Long> matchCounts = new HashMap<>();
			for (WebOperationRequestPredicate predicate : predicates) {
				matchCounts.put(predicate, Stream.of(matchers).filter((matcher) -> matcher.matches(predicate)).count());
			}
			return matchCounts.values().stream().noneMatch((count) -> count != 1);
		}, Arrays.toString(matchers));
	}

	private RequestPredicateMatcher path(String path) {
		return new RequestPredicateMatcher(path);
	}

	@Configuration(proxyBeanMethods = false)
	static class EmptyConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	static class MultipleEndpointsConfiguration {

		@Bean
		TestEndpoint testEndpoint() {
			return new TestEndpoint();
		}

		@Bean
		NonWebEndpoint nonWebEndpoint() {
			return new NonWebEndpoint();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TestWebEndpointExtensionConfiguration {

		@Bean
		TestWebEndpointExtension endpointExtension() {
			return new TestWebEndpointExtension();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ClashingOperationsEndpointConfiguration {

		@Bean
		ClashingOperationsEndpoint clashingOperationsEndpoint() {
			return new ClashingOperationsEndpoint();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ClashingOperationsWebEndpointExtensionConfiguration {

		@Bean
		ClashingOperationsWebEndpointExtension clashingOperationsExtension() {
			return new ClashingOperationsWebEndpointExtension();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(TestEndpointConfiguration.class)
	static class OverriddenOperationWebEndpointExtensionConfiguration {

		@Bean
		OverriddenOperationWebEndpointExtension overriddenOperationExtension() {
			return new OverriddenOperationWebEndpointExtension();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(TestEndpointConfiguration.class)
	static class AdditionalOperationWebEndpointConfiguration {

		@Bean
		AdditionalOperationWebEndpointExtension additionalOperationExtension() {
			return new AdditionalOperationWebEndpointExtension();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TestEndpointConfiguration {

		@Bean
		TestEndpoint testEndpoint() {
			return new TestEndpoint();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ClashingWebEndpointConfiguration {

		@Bean
		TestEndpoint testEndpoint() {
			return new TestEndpoint();
		}

		@Bean
		TestWebEndpointExtension testExtensionOne() {
			return new TestWebEndpointExtension();
		}

		@Bean
		TestWebEndpointExtension testExtensionTwo() {
			return new TestWebEndpointExtension();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ClashingStandardEndpointConfiguration {

		@Bean
		TestEndpoint testEndpointTwo() {
			return new TestEndpoint();
		}

		@Bean
		TestEndpoint testEndpointOne() {
			return new TestEndpoint();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ClashingSelectorsWebEndpointExtensionConfiguration {

		@Bean
		TestEndpoint testEndpoint() {
			return new TestEndpoint();
		}

		@Bean
		ClashingSelectorsWebEndpointExtension clashingSelectorsExtension() {
			return new ClashingSelectorsWebEndpointExtension();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class InvalidWebExtensionConfiguration {

		@Bean
		NonWebEndpoint nonWebEndpoint() {
			return new NonWebEndpoint();
		}

		@Bean
		NonWebWebEndpointExtension nonWebWebEndpointExtension() {
			return new NonWebWebEndpointExtension();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class VoidWriteOperationEndpointConfiguration {

		@Bean
		VoidWriteOperationEndpoint voidWriteOperationEndpoint() {
			return new VoidWriteOperationEndpoint();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseConfiguration.class)
	static class ResourceEndpointConfiguration {

		@Bean
		ResourceEndpoint resourceEndpoint() {
			return new ResourceEndpoint();
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

	@EndpointWebExtension(endpoint = TestEndpoint.class)
	static class TestWebEndpointExtension {

		@ReadOperation
		Object getAll() {
			return null;
		}

		@ReadOperation
		Object getOne(@Selector String id) {
			return null;
		}

		@WriteOperation
		void update(String foo, String bar) {

		}

		void someOtherMethod() {

		}

	}

	@Endpoint(id = "test")
	static class TestEndpoint {

		@ReadOperation
		Object getAll() {
			return null;
		}

	}

	@EndpointWebExtension(endpoint = TestEndpoint.class)
	static class OverriddenOperationWebEndpointExtension {

		@ReadOperation
		Object getAll() {
			return null;
		}

	}

	@EndpointWebExtension(endpoint = TestEndpoint.class)
	static class AdditionalOperationWebEndpointExtension {

		@ReadOperation
		Object getOne(@Selector String id) {
			return null;
		}

	}

	@Endpoint(id = "test")
	static class ClashingOperationsEndpoint {

		@ReadOperation
		Object getAll() {
			return null;
		}

		@ReadOperation
		Object getAgain() {
			return null;
		}

	}

	@EndpointWebExtension(endpoint = TestEndpoint.class)
	static class ClashingOperationsWebEndpointExtension {

		@ReadOperation
		Object getAll() {
			return null;
		}

		@ReadOperation
		Object getAgain() {
			return null;
		}

	}

	@EndpointWebExtension(endpoint = TestEndpoint.class)
	static class ClashingSelectorsWebEndpointExtension {

		@ReadOperation
		Object readOne(@Selector String oneA, @Selector String oneB) {
			return null;
		}

		@ReadOperation
		Object readTwo(@Selector String twoA, @Selector String twoB) {
			return null;
		}

	}

	@JmxEndpoint(id = "nonweb")
	static class NonWebEndpoint {

		@ReadOperation
		Object getData() {
			return null;
		}

	}

	@EndpointWebExtension(endpoint = NonWebEndpoint.class)
	static class NonWebWebEndpointExtension {

		@ReadOperation
		Object getSomething(@Selector String name) {
			return null;
		}

	}

	@Endpoint(id = "voidwrite")
	static class VoidWriteOperationEndpoint {

		@WriteOperation
		void write(String foo, String bar) {
		}

	}

	@Endpoint(id = "resource")
	static class ResourceEndpoint {

		@ReadOperation
		Resource read() {
			return new ByteArrayResource(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 });
		}

	}

	@Endpoint(id = "custommediatypes")
	static class CustomMediaTypesEndpoint {

		@ReadOperation(produces = "text/plain")
		String read() {
			return "read";
		}

		@WriteOperation(produces = { "a/b", "c/d" })
		String write() {
			return "write";
		}

		@DeleteOperation(produces = "text/plain")
		String delete() {
			return "delete";
		}

	}

	private static final class RequestPredicateMatcher {

		private final String path;

		private List<String> produces;

		private List<String> consumes;

		private WebEndpointHttpMethod httpMethod;

		private RequestPredicateMatcher(String path) {
			this.path = path;
		}

		RequestPredicateMatcher produces(String... mediaTypes) {
			this.produces = Arrays.asList(mediaTypes);
			return this;
		}

		RequestPredicateMatcher consumes(String... mediaTypes) {
			this.consumes = Arrays.asList(mediaTypes);
			return this;
		}

		private RequestPredicateMatcher httpMethod(WebEndpointHttpMethod httpMethod) {
			this.httpMethod = httpMethod;
			return this;
		}

		private boolean matches(WebOperationRequestPredicate predicate) {
			return (this.path == null || this.path.equals(predicate.getPath()))
					&& (this.httpMethod == null || this.httpMethod == predicate.getHttpMethod())
					&& (this.produces == null || this.produces.equals(new ArrayList<>(predicate.getProduces())))
					&& (this.consumes == null || this.consumes.equals(new ArrayList<>(predicate.getConsumes())));
		}

		@Override
		public String toString() {
			return "Request predicate with path = '" + this.path + "', httpMethod = '" + this.httpMethod
					+ "', produces = '" + this.produces + "'";
		}

	}

}
