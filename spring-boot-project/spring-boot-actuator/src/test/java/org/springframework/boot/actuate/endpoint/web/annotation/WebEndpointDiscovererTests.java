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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.assertj.core.api.Condition;
import org.junit.Test;

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
 */
public class WebEndpointDiscovererTests {

	@Test
	public void getEndpointsWhenNoEndpointBeansShouldReturnEmptyCollection() {
		load(EmptyConfiguration.class,
				(discoverer) -> assertThat(discoverer.getEndpoints()).isEmpty());
	}

	@Test
	public void getEndpointsWhenWebExtensionIsMissingEndpointShouldThrowException() {
		load(TestWebEndpointExtensionConfiguration.class,
				(discoverer) -> assertThatIllegalStateException()
						.isThrownBy(discoverer::getEndpoints).withMessageContaining(
								"Invalid extension 'endpointExtension': no endpoint found with id '"
										+ "test'"));
	}

	@Test
	public void getEndpointsWhenHasFilteredEndpointShouldOnlyDiscoverWebEndpoints() {
		load(MultipleEndpointsConfiguration.class, (discoverer) -> {
			Map<EndpointId, ExposableWebEndpoint> endpoints = mapEndpoints(
					discoverer.getEndpoints());
			assertThat(endpoints).containsOnlyKeys(EndpointId.of("test"));
		});
	}

	@Test
	public void getEndpointsWhenHasWebExtensionShouldOverrideStandardEndpoint() {
		load(OverriddenOperationWebEndpointExtensionConfiguration.class, (discoverer) -> {
			Map<EndpointId, ExposableWebEndpoint> endpoints = mapEndpoints(
					discoverer.getEndpoints());
			assertThat(endpoints).containsOnlyKeys(EndpointId.of("test"));
			ExposableWebEndpoint endpoint = endpoints.get(EndpointId.of("test"));
			assertThat(requestPredicates(endpoint)).has(
					requestPredicates(path("test").httpMethod(WebEndpointHttpMethod.GET)
							.consumes().produces("application/json")));
		});
	}

	@Test
	public void getEndpointsWhenExtensionAddsOperationShouldHaveBothOperations() {
		load(AdditionalOperationWebEndpointConfiguration.class, (discoverer) -> {
			Map<EndpointId, ExposableWebEndpoint> endpoints = mapEndpoints(
					discoverer.getEndpoints());
			assertThat(endpoints).containsOnlyKeys(EndpointId.of("test"));
			ExposableWebEndpoint endpoint = endpoints.get(EndpointId.of("test"));
			assertThat(requestPredicates(endpoint)).has(requestPredicates(
					path("test").httpMethod(WebEndpointHttpMethod.GET).consumes()
							.produces("application/json"),
					path("test/{id}").httpMethod(WebEndpointHttpMethod.GET).consumes()
							.produces("application/json")));
		});
	}

	@Test
	public void getEndpointsWhenPredicateForWriteOperationThatReturnsVoidShouldHaveNoProducedMediaTypes() {
		load(VoidWriteOperationEndpointConfiguration.class, (discoverer) -> {
			Map<EndpointId, ExposableWebEndpoint> endpoints = mapEndpoints(
					discoverer.getEndpoints());
			assertThat(endpoints).containsOnlyKeys(EndpointId.of("voidwrite"));
			ExposableWebEndpoint endpoint = endpoints.get(EndpointId.of("voidwrite"));
			assertThat(requestPredicates(endpoint)).has(requestPredicates(
					path("voidwrite").httpMethod(WebEndpointHttpMethod.POST).produces()
							.consumes("application/json")));
		});
	}

	@Test
	public void getEndpointsWhenTwoExtensionsHaveTheSameEndpointTypeShouldThrowException() {
		load(ClashingWebEndpointConfiguration.class,
				(discoverer) -> assertThatIllegalStateException()
						.isThrownBy(discoverer::getEndpoints).withMessageContaining(
								"Found multiple extensions for the endpoint bean "
										+ "testEndpoint (testExtensionOne, testExtensionTwo)"));
	}

	@Test
	public void getEndpointsWhenTwoStandardEndpointsHaveTheSameIdShouldThrowException() {
		load(ClashingStandardEndpointConfiguration.class,
				(discoverer) -> assertThatIllegalStateException()
						.isThrownBy(discoverer::getEndpoints).withMessageContaining(
								"Found two endpoints with the id 'test': "));
	}

	@Test
	public void getEndpointsWhenWhenEndpointHasTwoOperationsWithTheSameNameShouldThrowException() {
		load(ClashingOperationsEndpointConfiguration.class,
				(discoverer) -> assertThatIllegalStateException()
						.isThrownBy(discoverer::getEndpoints).withMessageContaining(
								"Unable to map duplicate endpoint operations: "
										+ "[web request predicate GET to path 'test' "
										+ "produces: application/json] to clashingOperationsEndpoint"));
	}

	@Test
	public void getEndpointsWhenExtensionIsNotCompatibleWithTheEndpointTypeShouldThrowException() {
		load(InvalidWebExtensionConfiguration.class,
				(discoverer) -> assertThatIllegalStateException()
						.isThrownBy(discoverer::getEndpoints).withMessageContaining(
								"Endpoint bean 'nonWebEndpoint' cannot support the "
										+ "extension bean 'nonWebWebEndpointExtension'"));
	}

	@Test
	public void getEndpointsWhenWhenExtensionHasTwoOperationsWithTheSameNameShouldThrowException() {
		load(ClashingSelectorsWebEndpointExtensionConfiguration.class,
				(discoverer) -> assertThatIllegalStateException()
						.isThrownBy(discoverer::getEndpoints)
						.withMessageContaining(
								"Unable to map duplicate endpoint operations")
						.withMessageContaining(
								"to testEndpoint (clashingSelectorsExtension)"));
	}

	@Test
	public void getEndpointsWhenHasCacheWithTtlShouldCacheReadOperationWithTtlValue() {
		load((id) -> 500L, EndpointId::toString, TestEndpointConfiguration.class,
				(discoverer) -> {
					Map<EndpointId, ExposableWebEndpoint> endpoints = mapEndpoints(
							discoverer.getEndpoints());
					assertThat(endpoints).containsOnlyKeys(EndpointId.of("test"));
					ExposableWebEndpoint endpoint = endpoints.get(EndpointId.of("test"));
					assertThat(endpoint.getOperations()).hasSize(1);
					WebOperation operation = endpoint.getOperations().iterator().next();
					Object invoker = ReflectionTestUtils.getField(operation, "invoker");
					assertThat(invoker).isInstanceOf(CachingOperationInvoker.class);
					assertThat(((CachingOperationInvoker) invoker).getTimeToLive())
							.isEqualTo(500);
				});
	}

	@Test
	public void getEndpointsWhenOperationReturnsResourceShouldProduceApplicationOctetStream() {
		load(ResourceEndpointConfiguration.class, (discoverer) -> {
			Map<EndpointId, ExposableWebEndpoint> endpoints = mapEndpoints(
					discoverer.getEndpoints());
			assertThat(endpoints).containsOnlyKeys(EndpointId.of("resource"));
			ExposableWebEndpoint endpoint = endpoints.get(EndpointId.of("resource"));
			assertThat(requestPredicates(endpoint)).has(requestPredicates(
					path("resource").httpMethod(WebEndpointHttpMethod.GET).consumes()
							.produces("application/octet-stream")));
		});
	}

	@Test
	public void getEndpointsWhenHasCustomMediaTypeShouldProduceCustomMediaType() {
		load(CustomMediaTypesEndpointConfiguration.class, (discoverer) -> {
			Map<EndpointId, ExposableWebEndpoint> endpoints = mapEndpoints(
					discoverer.getEndpoints());
			assertThat(endpoints).containsOnlyKeys(EndpointId.of("custommediatypes"));
			ExposableWebEndpoint endpoint = endpoints
					.get(EndpointId.of("custommediatypes"));
			assertThat(requestPredicates(endpoint)).has(requestPredicates(
					path("custommediatypes").httpMethod(WebEndpointHttpMethod.GET)
							.consumes().produces("text/plain"),
					path("custommediatypes").httpMethod(WebEndpointHttpMethod.POST)
							.consumes().produces("a/b", "c/d"),
					path("custommediatypes").httpMethod(WebEndpointHttpMethod.DELETE)
							.consumes().produces("text/plain")));
		});
	}

	@Test
	public void getEndpointsWhenHasCustomPathShouldReturnCustomPath() {
		load((id) -> null, (id) -> "custom/" + id,
				AdditionalOperationWebEndpointConfiguration.class, (discoverer) -> {
					Map<EndpointId, ExposableWebEndpoint> endpoints = mapEndpoints(
							discoverer.getEndpoints());
					assertThat(endpoints).containsOnlyKeys(EndpointId.of("test"));
					ExposableWebEndpoint endpoint = endpoints.get(EndpointId.of("test"));
					Condition<List<? extends WebOperationRequestPredicate>> expected = requestPredicates(
							path("custom/test").httpMethod(WebEndpointHttpMethod.GET)
									.consumes().produces("application/json"),
							path("custom/test/{id}").httpMethod(WebEndpointHttpMethod.GET)
									.consumes().produces("application/json"));
					assertThat(requestPredicates(endpoint)).has(expected);
				});
	}

	private void load(Class<?> configuration, Consumer<WebEndpointDiscoverer> consumer) {
		this.load((id) -> null, EndpointId::toString, configuration, consumer);
	}

	private void load(Function<EndpointId, Long> timeToLive,
			PathMapper endpointPathMapper, Class<?> configuration,
			Consumer<WebEndpointDiscoverer> consumer) {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				configuration)) {
			ConversionServiceParameterValueMapper parameterMapper = new ConversionServiceParameterValueMapper(
					DefaultConversionService.getSharedInstance());
			EndpointMediaTypes mediaTypes = new EndpointMediaTypes(
					Collections.singletonList("application/json"),
					Collections.singletonList("application/json"));
			WebEndpointDiscoverer discoverer = new WebEndpointDiscoverer(context,
					parameterMapper, mediaTypes,
					Collections.singletonList(endpointPathMapper),
					Collections.singleton(new CachingOperationInvokerAdvisor(timeToLive)),
					Collections.emptyList());
			consumer.accept(discoverer);
		}
	}

	private Map<EndpointId, ExposableWebEndpoint> mapEndpoints(
			Collection<ExposableWebEndpoint> endpoints) {
		Map<EndpointId, ExposableWebEndpoint> endpointById = new HashMap<>();
		endpoints.forEach(
				(endpoint) -> endpointById.put(endpoint.getEndpointId(), endpoint));
		return endpointById;
	}

	private List<WebOperationRequestPredicate> requestPredicates(
			ExposableWebEndpoint endpoint) {
		return endpoint.getOperations().stream().map(WebOperation::getRequestPredicate)
				.collect(Collectors.toList());
	}

	private Condition<List<? extends WebOperationRequestPredicate>> requestPredicates(
			RequestPredicateMatcher... matchers) {
		return new Condition<>((predicates) -> {
			if (predicates.size() != matchers.length) {
				return false;
			}
			Map<WebOperationRequestPredicate, Long> matchCounts = new HashMap<>();
			for (WebOperationRequestPredicate predicate : predicates) {
				matchCounts.put(predicate, Stream.of(matchers)
						.filter((matcher) -> matcher.matches(predicate)).count());
			}
			return matchCounts.values().stream().noneMatch((count) -> count != 1);
		}, Arrays.toString(matchers));
	}

	private RequestPredicateMatcher path(String path) {
		return new RequestPredicateMatcher(path);
	}

	@Configuration
	static class EmptyConfiguration {

	}

	@Configuration
	static class MultipleEndpointsConfiguration {

		@Bean
		public TestEndpoint testEndpoint() {
			return new TestEndpoint();
		}

		@Bean
		public NonWebEndpoint nonWebEndpoint() {
			return new NonWebEndpoint();
		}

	}

	@Configuration
	static class TestWebEndpointExtensionConfiguration {

		@Bean
		public TestWebEndpointExtension endpointExtension() {
			return new TestWebEndpointExtension();
		}

	}

	@Configuration
	static class ClashingOperationsEndpointConfiguration {

		@Bean
		public ClashingOperationsEndpoint clashingOperationsEndpoint() {
			return new ClashingOperationsEndpoint();
		}

	}

	@Configuration
	static class ClashingOperationsWebEndpointExtensionConfiguration {

		@Bean
		public ClashingOperationsWebEndpointExtension clashingOperationsExtension() {
			return new ClashingOperationsWebEndpointExtension();
		}

	}

	@Configuration
	@Import(TestEndpointConfiguration.class)
	static class OverriddenOperationWebEndpointExtensionConfiguration {

		@Bean
		public OverriddenOperationWebEndpointExtension overriddenOperationExtension() {
			return new OverriddenOperationWebEndpointExtension();
		}

	}

	@Configuration
	@Import(TestEndpointConfiguration.class)
	static class AdditionalOperationWebEndpointConfiguration {

		@Bean
		public AdditionalOperationWebEndpointExtension additionalOperationExtension() {
			return new AdditionalOperationWebEndpointExtension();
		}

	}

	@Configuration
	static class TestEndpointConfiguration {

		@Bean
		public TestEndpoint testEndpoint() {
			return new TestEndpoint();
		}

	}

	@Configuration
	static class ClashingWebEndpointConfiguration {

		@Bean
		public TestEndpoint testEndpoint() {
			return new TestEndpoint();
		}

		@Bean
		public TestWebEndpointExtension testExtensionOne() {
			return new TestWebEndpointExtension();
		}

		@Bean
		public TestWebEndpointExtension testExtensionTwo() {
			return new TestWebEndpointExtension();
		}

	}

	@Configuration
	static class ClashingStandardEndpointConfiguration {

		@Bean
		public TestEndpoint testEndpointTwo() {
			return new TestEndpoint();
		}

		@Bean
		public TestEndpoint testEndpointOne() {
			return new TestEndpoint();
		}

	}

	@Configuration
	static class ClashingSelectorsWebEndpointExtensionConfiguration {

		@Bean
		public TestEndpoint testEndpoint() {
			return new TestEndpoint();
		}

		@Bean
		public ClashingSelectorsWebEndpointExtension clashingSelectorsExtension() {
			return new ClashingSelectorsWebEndpointExtension();
		}

	}

	@Configuration
	static class InvalidWebExtensionConfiguration {

		@Bean
		public NonWebEndpoint nonWebEndpoint() {
			return new NonWebEndpoint();
		}

		@Bean
		public NonWebWebEndpointExtension nonWebWebEndpointExtension() {
			return new NonWebWebEndpointExtension();
		}

	}

	@Configuration
	static class VoidWriteOperationEndpointConfiguration {

		@Bean
		public VoidWriteOperationEndpoint voidWriteOperationEndpoint() {
			return new VoidWriteOperationEndpoint();
		}

	}

	@Configuration
	@Import(BaseConfiguration.class)
	static class ResourceEndpointConfiguration {

		@Bean
		public ResourceEndpoint resourceEndpoint() {
			return new ResourceEndpoint();
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

	@EndpointWebExtension(endpoint = TestEndpoint.class)
	static class TestWebEndpointExtension {

		@ReadOperation
		public Object getAll() {
			return null;
		}

		@ReadOperation
		public Object getOne(@Selector String id) {
			return null;
		}

		@WriteOperation
		public void update(String foo, String bar) {

		}

		public void someOtherMethod() {

		}

	}

	@Endpoint(id = "test")
	static class TestEndpoint {

		@ReadOperation
		public Object getAll() {
			return null;
		}

	}

	@EndpointWebExtension(endpoint = TestEndpoint.class)
	static class OverriddenOperationWebEndpointExtension {

		@ReadOperation
		public Object getAll() {
			return null;
		}

	}

	@EndpointWebExtension(endpoint = TestEndpoint.class)
	static class AdditionalOperationWebEndpointExtension {

		@ReadOperation
		public Object getOne(@Selector String id) {
			return null;
		}

	}

	@Endpoint(id = "test")
	static class ClashingOperationsEndpoint {

		@ReadOperation
		public Object getAll() {
			return null;
		}

		@ReadOperation
		public Object getAgain() {
			return null;
		}

	}

	@EndpointWebExtension(endpoint = TestEndpoint.class)
	static class ClashingOperationsWebEndpointExtension {

		@ReadOperation
		public Object getAll() {
			return null;
		}

		@ReadOperation
		public Object getAgain() {
			return null;
		}

	}

	@EndpointWebExtension(endpoint = TestEndpoint.class)
	static class ClashingSelectorsWebEndpointExtension {

		@ReadOperation
		public Object readOne(@Selector String oneA, @Selector String oneB) {
			return null;
		}

		@ReadOperation
		public Object readTwo(@Selector String twoA, @Selector String twoB) {
			return null;
		}

	}

	@JmxEndpoint(id = "nonweb")
	static class NonWebEndpoint {

		@ReadOperation
		public Object getData() {
			return null;
		}

	}

	@EndpointWebExtension(endpoint = NonWebEndpoint.class)
	static class NonWebWebEndpointExtension {

		@ReadOperation
		public Object getSomething(@Selector String name) {
			return null;
		}

	}

	@Endpoint(id = "voidwrite")
	static class VoidWriteOperationEndpoint {

		@WriteOperation
		public void write(String foo, String bar) {
		}

	}

	@Endpoint(id = "resource")
	static class ResourceEndpoint {

		@ReadOperation
		public Resource read() {
			return new ByteArrayResource(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 });
		}

	}

	@Endpoint(id = "custommediatypes")
	static class CustomMediaTypesEndpoint {

		@ReadOperation(produces = "text/plain")
		public String read() {
			return "read";
		}

		@WriteOperation(produces = { "a/b", "c/d" })
		public String write() {
			return "write";

		}

		@DeleteOperation(produces = "text/plain")
		public String delete() {
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

		public RequestPredicateMatcher produces(String... mediaTypes) {
			this.produces = Arrays.asList(mediaTypes);
			return this;
		}

		public RequestPredicateMatcher consumes(String... mediaTypes) {
			this.consumes = Arrays.asList(mediaTypes);
			return this;
		}

		private RequestPredicateMatcher httpMethod(WebEndpointHttpMethod httpMethod) {
			this.httpMethod = httpMethod;
			return this;
		}

		private boolean matches(WebOperationRequestPredicate predicate) {
			return (this.path == null || this.path.equals(predicate.getPath()))
					&& (this.httpMethod == null
							|| this.httpMethod == predicate.getHttpMethod())
					&& (this.produces == null || this.produces
							.equals(new ArrayList<>(predicate.getProduces())))
					&& (this.consumes == null || this.consumes
							.equals(new ArrayList<>(predicate.getConsumes())));
		}

		@Override
		public String toString() {
			return "Request predicate with path = '" + this.path + "', httpMethod = '"
					+ this.httpMethod + "', produces = '" + this.produces + "'";
		}

	}

}
