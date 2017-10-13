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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.actuate.endpoint.EndpointInfo;
import org.springframework.boot.actuate.endpoint.OperationInvoker;
import org.springframework.boot.actuate.endpoint.annotation.DeleteOperation;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.boot.actuate.endpoint.cache.CachingOperationInvoker;
import org.springframework.boot.actuate.endpoint.cache.CachingOperationInvokerAdvisor;
import org.springframework.boot.actuate.endpoint.convert.ConversionServiceParameterMapper;
import org.springframework.boot.actuate.endpoint.jmx.annotation.JmxEndpoint;
import org.springframework.boot.actuate.endpoint.web.AbstractWebEndpointIntegrationTests.BaseConfiguration;
import org.springframework.boot.actuate.endpoint.web.annotation.EndpointWebExtension;
import org.springframework.boot.actuate.endpoint.web.annotation.WebAnnotationEndpointDiscoverer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebAnnotationEndpointDiscoverer}.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 */
public class WebAnnotationEndpointDiscovererTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	@Test
	public void discoveryWorksWhenThereAreNoEndpoints() {
		load(EmptyConfiguration.class,
				(discoverer) -> assertThat(discoverer.discoverEndpoints()).isEmpty());
	}

	@Test
	public void webExtensionMustHaveEndpoint() {
		load(TestWebEndpointExtensionConfiguration.class, (discoverer) -> {
			this.thrown.expect(IllegalStateException.class);
			this.thrown.expectMessage("Invalid extension");
			this.thrown.expectMessage(TestWebEndpointExtension.class.getName());
			this.thrown.expectMessage("no endpoint found");
			this.thrown.expectMessage(TestEndpoint.class.getName());
			discoverer.discoverEndpoints();
		});
	}

	@Test
	public void onlyWebEndpointsAreDiscovered() {
		load(MultipleEndpointsConfiguration.class, (discoverer) -> {
			Map<String, EndpointInfo<WebOperation>> endpoints = mapEndpoints(
					discoverer.discoverEndpoints());
			assertThat(endpoints).containsOnlyKeys("test");
		});
	}

	@Test
	public void oneOperationIsDiscoveredWhenExtensionOverridesOperation() {
		load(OverriddenOperationWebEndpointExtensionConfiguration.class, (discoverer) -> {
			Map<String, EndpointInfo<WebOperation>> endpoints = mapEndpoints(
					discoverer.discoverEndpoints());
			assertThat(endpoints).containsOnlyKeys("test");
			EndpointInfo<WebOperation> endpoint = endpoints.get("test");
			assertThat(requestPredicates(endpoint)).has(
					requestPredicates(path("test").httpMethod(WebEndpointHttpMethod.GET)
							.consumes().produces("application/json")));
		});
	}

	@Test
	public void twoOperationsAreDiscoveredWhenExtensionAddsOperation() {
		load(AdditionalOperationWebEndpointConfiguration.class, (discoverer) -> {
			Map<String, EndpointInfo<WebOperation>> endpoints = mapEndpoints(
					discoverer.discoverEndpoints());
			assertThat(endpoints).containsOnlyKeys("test");
			EndpointInfo<WebOperation> endpoint = endpoints.get("test");
			assertThat(requestPredicates(endpoint)).has(requestPredicates(
					path("test").httpMethod(WebEndpointHttpMethod.GET).consumes()
							.produces("application/json"),
					path("test/{id}").httpMethod(WebEndpointHttpMethod.GET).consumes()
							.produces("application/json")));
		});
	}

	@Test
	public void predicateForWriteOperationThatReturnsVoidHasNoProducedMediaTypes() {
		load(VoidWriteOperationEndpointConfiguration.class, (discoverer) -> {
			Map<String, EndpointInfo<WebOperation>> endpoints = mapEndpoints(
					discoverer.discoverEndpoints());
			assertThat(endpoints).containsOnlyKeys("voidwrite");
			EndpointInfo<WebOperation> endpoint = endpoints.get("voidwrite");
			assertThat(requestPredicates(endpoint)).has(requestPredicates(
					path("voidwrite").httpMethod(WebEndpointHttpMethod.POST).produces()
							.consumes("application/json")));
		});
	}

	@Test
	public void discoveryFailsWhenTwoExtensionsHaveTheSameEndpointType() {
		load(ClashingWebEndpointConfiguration.class, (discoverer) -> {
			this.thrown.expect(IllegalStateException.class);
			this.thrown.expectMessage("Found two extensions for the same endpoint");
			this.thrown.expectMessage(TestEndpoint.class.getName());
			this.thrown.expectMessage(TestWebEndpointExtension.class.getName());
			discoverer.discoverEndpoints();
		});
	}

	@Test
	public void discoveryFailsWhenTwoStandardEndpointsHaveTheSameId() {
		load(ClashingStandardEndpointConfiguration.class, (discoverer) -> {
			this.thrown.expect(IllegalStateException.class);
			this.thrown.expectMessage("Found two endpoints with the id 'test': ");
			discoverer.discoverEndpoints();
		});
	}

	@Test
	public void discoveryFailsWhenEndpointHasClashingOperations() {
		load(ClashingOperationsEndpointConfiguration.class, (discoverer) -> {
			this.thrown.expect(IllegalStateException.class);
			this.thrown.expectMessage(
					"Found multiple web operations with matching request predicates:");
			discoverer.discoverEndpoints();
		});
	}

	@Test
	public void discoveryFailsWhenExtensionIsNotCompatibleWithTheEndpointType() {
		load(InvalidWebExtensionConfiguration.class, (discoverer) -> {
			this.thrown.expect(IllegalStateException.class);
			this.thrown.expectMessage("Invalid extension");
			this.thrown.expectMessage(NonWebWebEndpointExtension.class.getName());
			this.thrown.expectMessage(NonWebEndpoint.class.getName());
			discoverer.discoverEndpoints();
		});
	}

	@Test
	public void twoOperationsOnSameEndpointClashWhenSelectorsHaveDifferentNames() {
		load(ClashingSelectorsWebEndpointExtensionConfiguration.class, (discoverer) -> {
			this.thrown.expect(IllegalStateException.class);
			this.thrown.expectMessage(
					"Found multiple web operations with matching request predicates:");
			discoverer.discoverEndpoints();
		});
	}

	@Test
	public void endpointMainReadOperationIsCachedWithMatchingId() {
		load((id) -> 500L, (id) -> id, TestEndpointConfiguration.class, (discoverer) -> {
			Map<String, EndpointInfo<WebOperation>> endpoints = mapEndpoints(
					discoverer.discoverEndpoints());
			assertThat(endpoints).containsOnlyKeys("test");
			EndpointInfo<WebOperation> endpoint = endpoints.get("test");
			assertThat(endpoint.getOperations()).hasSize(1);
			OperationInvoker operationInvoker = endpoint.getOperations().iterator().next()
					.getInvoker();
			assertThat(operationInvoker).isInstanceOf(CachingOperationInvoker.class);
			assertThat(((CachingOperationInvoker) operationInvoker).getTimeToLive())
					.isEqualTo(500);
		});
	}

	@Test
	public void operationsThatReturnResourceProduceApplicationOctetStream() {
		load(ResourceEndpointConfiguration.class, (discoverer) -> {
			Map<String, EndpointInfo<WebOperation>> endpoints = mapEndpoints(
					discoverer.discoverEndpoints());
			assertThat(endpoints).containsOnlyKeys("resource");
			EndpointInfo<WebOperation> endpoint = endpoints.get("resource");
			assertThat(requestPredicates(endpoint)).has(requestPredicates(
					path("resource").httpMethod(WebEndpointHttpMethod.GET).consumes()
							.produces("application/octet-stream")));
		});
	}

	@Test
	public void operationCanProduceCustomMediaTypes() {
		load(CustomMediaTypesEndpointConfiguration.class, (discoverer) -> {
			Map<String, EndpointInfo<WebOperation>> endpoints = mapEndpoints(
					discoverer.discoverEndpoints());
			assertThat(endpoints).containsOnlyKeys("custommediatypes");
			EndpointInfo<WebOperation> endpoint = endpoints.get("custommediatypes");
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
	public void endpointPathCanBeCustomized() {
		load((id) -> null, (id) -> "custom/" + id,
				AdditionalOperationWebEndpointConfiguration.class, (discoverer) -> {
					Map<String, EndpointInfo<WebOperation>> endpoints = mapEndpoints(
							discoverer.discoverEndpoints());
					assertThat(endpoints).containsOnlyKeys("test");
					EndpointInfo<WebOperation> endpoint = endpoints.get("test");
					Condition<List<? extends OperationRequestPredicate>> expected = requestPredicates(
							path("custom/test").httpMethod(WebEndpointHttpMethod.GET)
									.consumes().produces("application/json"),
							path("custom/test/{id}").httpMethod(WebEndpointHttpMethod.GET)
									.consumes().produces("application/json"));
					assertThat(requestPredicates(endpoint)).has(expected);
				});
	}

	private void load(Class<?> configuration,
			Consumer<WebAnnotationEndpointDiscoverer> consumer) {
		this.load((id) -> null, (id) -> id, configuration, consumer);
	}

	private void load(Function<String, Long> timeToLive,
			EndpointPathResolver endpointPathResolver, Class<?> configuration,
			Consumer<WebAnnotationEndpointDiscoverer> consumer) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				configuration);
		try {
			ConversionServiceParameterMapper parameterMapper = new ConversionServiceParameterMapper(
					DefaultConversionService.getSharedInstance());
			EndpointMediaTypes mediaTypes = new EndpointMediaTypes(
					Collections.singletonList("application/json"),
					Collections.singletonList("application/json"));
			WebAnnotationEndpointDiscoverer discoverer = new WebAnnotationEndpointDiscoverer(
					context, parameterMapper, mediaTypes, endpointPathResolver,
					Collections.singleton(new CachingOperationInvokerAdvisor(timeToLive)),
					null);
			consumer.accept(discoverer);
		}
		finally {
			context.close();
		}
	}

	private Map<String, EndpointInfo<WebOperation>> mapEndpoints(
			Collection<EndpointInfo<WebOperation>> endpoints) {
		Map<String, EndpointInfo<WebOperation>> endpointById = new HashMap<>();
		endpoints.forEach((endpoint) -> endpointById.put(endpoint.getId(), endpoint));
		return endpointById;
	}

	private List<OperationRequestPredicate> requestPredicates(
			EndpointInfo<WebOperation> endpoint) {
		return endpoint.getOperations().stream().map(WebOperation::getRequestPredicate)
				.collect(Collectors.toList());
	}

	private Condition<List<? extends OperationRequestPredicate>> requestPredicates(
			RequestPredicateMatcher... matchers) {
		return new Condition<>((predicates) -> {
			if (predicates.size() != matchers.length) {
				return false;
			}
			Map<OperationRequestPredicate, Long> matchCounts = new HashMap<>();
			for (OperationRequestPredicate predicate : predicates) {
				matchCounts.put(predicate, Stream.of(matchers)
						.filter(matcher -> matcher.matches(predicate)).count());
			}
			return matchCounts.values().stream().noneMatch(count -> count != 1);
		}, Arrays.toString(matchers));
	}

	private RequestPredicateMatcher path(String path) {
		return new RequestPredicateMatcher(path);
	}

	@Configuration
	static class EmptyConfiguration {

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

		private boolean matches(OperationRequestPredicate predicate) {
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
