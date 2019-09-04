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

package org.springframework.boot.actuate.endpoint.jmx.annotation;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.annotation.DeleteOperation;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.boot.actuate.endpoint.invoke.convert.ConversionServiceParameterValueMapper;
import org.springframework.boot.actuate.endpoint.invoker.cache.CachingOperationInvoker;
import org.springframework.boot.actuate.endpoint.invoker.cache.CachingOperationInvokerAdvisor;
import org.springframework.boot.actuate.endpoint.jmx.ExposableJmxEndpoint;
import org.springframework.boot.actuate.endpoint.jmx.JmxOperation;
import org.springframework.boot.actuate.endpoint.jmx.JmxOperationParameter;
import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpoint;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedOperationParameter;
import org.springframework.jmx.export.annotation.ManagedOperationParameters;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link JmxEndpointDiscoverer}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
class JmxEndpointDiscovererTests {

	@Test
	void getEndpointsWhenNoEndpointBeansShouldReturnEmptyCollection() {
		load(EmptyConfiguration.class, (discoverer) -> assertThat(discoverer.getEndpoints()).isEmpty());
	}

	@Test
	void getEndpointsShouldDiscoverStandardEndpoints() {
		load(TestEndpoint.class, (discoverer) -> {
			Map<EndpointId, ExposableJmxEndpoint> endpoints = discover(discoverer);
			assertThat(endpoints).containsOnlyKeys(EndpointId.of("test"));
			Map<String, JmxOperation> operationByName = mapOperations(
					endpoints.get(EndpointId.of("test")).getOperations());
			assertThat(operationByName).containsOnlyKeys("getAll", "getSomething", "update", "deleteSomething");
			JmxOperation getAll = operationByName.get("getAll");
			assertThat(getAll.getDescription()).isEqualTo("Invoke getAll for endpoint test");
			assertThat(getAll.getOutputType()).isEqualTo(Object.class);
			assertThat(getAll.getParameters()).isEmpty();
			JmxOperation getSomething = operationByName.get("getSomething");
			assertThat(getSomething.getDescription()).isEqualTo("Invoke getSomething for endpoint test");
			assertThat(getSomething.getOutputType()).isEqualTo(String.class);
			assertThat(getSomething.getParameters()).hasSize(1);
			hasDefaultParameter(getSomething, 0, String.class);
			JmxOperation update = operationByName.get("update");
			assertThat(update.getDescription()).isEqualTo("Invoke update for endpoint test");
			assertThat(update.getOutputType()).isEqualTo(Void.TYPE);
			assertThat(update.getParameters()).hasSize(2);
			hasDefaultParameter(update, 0, String.class);
			hasDefaultParameter(update, 1, String.class);
			JmxOperation deleteSomething = operationByName.get("deleteSomething");
			assertThat(deleteSomething.getDescription()).isEqualTo("Invoke deleteSomething for endpoint test");
			assertThat(deleteSomething.getOutputType()).isEqualTo(Void.TYPE);
			assertThat(deleteSomething.getParameters()).hasSize(1);
			hasDefaultParameter(deleteSomething, 0, String.class);
		});
	}

	@Test
	void getEndpointsWhenHasFilteredEndpointShouldOnlyDiscoverJmxEndpoints() {
		load(MultipleEndpointsConfiguration.class, (discoverer) -> {
			Map<EndpointId, ExposableJmxEndpoint> endpoints = discover(discoverer);
			assertThat(endpoints).containsOnlyKeys(EndpointId.of("test"), EndpointId.of("jmx"));
		});
	}

	@Test
	void getEndpointsWhenJmxExtensionIsMissingEndpointShouldThrowException() {
		load(TestJmxEndpointExtension.class, (discoverer) -> assertThatIllegalStateException()
				.isThrownBy(discoverer::getEndpoints).withMessageContaining(
						"Invalid extension 'jmxEndpointDiscovererTests.TestJmxEndpointExtension': no endpoint found with id 'test'"));
	}

	@Test
	void getEndpointsWhenHasJmxExtensionShouldOverrideStandardEndpoint() {
		load(OverriddenOperationJmxEndpointConfiguration.class, (discoverer) -> {
			Map<EndpointId, ExposableJmxEndpoint> endpoints = discover(discoverer);
			assertThat(endpoints).containsOnlyKeys(EndpointId.of("test"));
			assertJmxTestEndpoint(endpoints.get(EndpointId.of("test")));
		});
	}

	@Test
	void getEndpointsWhenHasJmxExtensionWithNewOperationAddsExtraOperation() {
		load(AdditionalOperationJmxEndpointConfiguration.class, (discoverer) -> {
			Map<EndpointId, ExposableJmxEndpoint> endpoints = discover(discoverer);
			assertThat(endpoints).containsOnlyKeys(EndpointId.of("test"));
			Map<String, JmxOperation> operationByName = mapOperations(
					endpoints.get(EndpointId.of("test")).getOperations());
			assertThat(operationByName).containsOnlyKeys("getAll", "getSomething", "update", "deleteSomething",
					"getAnother");
			JmxOperation getAnother = operationByName.get("getAnother");
			assertThat(getAnother.getDescription()).isEqualTo("Get another thing");
			assertThat(getAnother.getOutputType()).isEqualTo(Object.class);
			assertThat(getAnother.getParameters()).isEmpty();
		});
	}

	@Test
	void getEndpointsWhenHasCacheWithTtlShouldCacheReadOperationWithTtlValue() {
		load(TestEndpoint.class, (id) -> 500L, (discoverer) -> {
			Map<EndpointId, ExposableJmxEndpoint> endpoints = discover(discoverer);
			assertThat(endpoints).containsOnlyKeys(EndpointId.of("test"));
			Map<String, JmxOperation> operationByName = mapOperations(
					endpoints.get(EndpointId.of("test")).getOperations());
			assertThat(operationByName).containsOnlyKeys("getAll", "getSomething", "update", "deleteSomething");
			JmxOperation getAll = operationByName.get("getAll");
			assertThat(getInvoker(getAll)).isInstanceOf(CachingOperationInvoker.class);
			assertThat(((CachingOperationInvoker) getInvoker(getAll)).getTimeToLive()).isEqualTo(500);
		});
	}

	@Test
	void getEndpointsShouldCacheReadOperations() {
		load(AdditionalOperationJmxEndpointConfiguration.class, (id) -> 500L, (discoverer) -> {
			Map<EndpointId, ExposableJmxEndpoint> endpoints = discover(discoverer);
			assertThat(endpoints).containsOnlyKeys(EndpointId.of("test"));
			Map<String, JmxOperation> operationByName = mapOperations(
					endpoints.get(EndpointId.of("test")).getOperations());
			assertThat(operationByName).containsOnlyKeys("getAll", "getSomething", "update", "deleteSomething",
					"getAnother");
			JmxOperation getAll = operationByName.get("getAll");
			assertThat(getInvoker(getAll)).isInstanceOf(CachingOperationInvoker.class);
			assertThat(((CachingOperationInvoker) getInvoker(getAll)).getTimeToLive()).isEqualTo(500);
			JmxOperation getAnother = operationByName.get("getAnother");
			assertThat(getInvoker(getAnother)).isInstanceOf(CachingOperationInvoker.class);
			assertThat(((CachingOperationInvoker) getInvoker(getAnother)).getTimeToLive()).isEqualTo(500);
		});
	}

	@Test
	void getEndpointsWhenTwoExtensionsHaveTheSameEndpointTypeShouldThrowException() {
		load(ClashingJmxEndpointConfiguration.class, (discoverer) -> assertThatIllegalStateException()
				.isThrownBy(discoverer::getEndpoints).withMessageContaining(
						"Found multiple extensions for the endpoint bean testEndpoint (testExtensionOne, testExtensionTwo)"));
	}

	@Test
	void getEndpointsWhenTwoStandardEndpointsHaveTheSameIdShouldThrowException() {
		load(ClashingStandardEndpointConfiguration.class,
				(discoverer) -> assertThatIllegalStateException().isThrownBy(discoverer::getEndpoints)
						.withMessageContaining("Found two endpoints with the id 'test': "));
	}

	@Test
	void getEndpointsWhenWhenEndpointHasTwoOperationsWithTheSameNameShouldThrowException() {
		load(ClashingOperationsEndpoint.class, (discoverer) -> assertThatIllegalStateException()
				.isThrownBy(discoverer::getEndpoints).withMessageContaining(
						"Unable to map duplicate endpoint operations: [MBean call 'getAll'] to jmxEndpointDiscovererTests.ClashingOperationsEndpoint"));
	}

	@Test
	void getEndpointsWhenWhenExtensionHasTwoOperationsWithTheSameNameShouldThrowException() {
		load(AdditionalClashingOperationsConfiguration.class, (discoverer) -> assertThatIllegalStateException()
				.isThrownBy(discoverer::getEndpoints).withMessageContaining(
						"Unable to map duplicate endpoint operations: [MBean call 'getAll'] to testEndpoint (clashingOperationsJmxEndpointExtension)"));
	}

	@Test
	void getEndpointsWhenExtensionIsNotCompatibleWithTheEndpointTypeShouldThrowException() {
		load(InvalidJmxExtensionConfiguration.class, (discoverer) -> assertThatIllegalStateException()
				.isThrownBy(discoverer::getEndpoints).withMessageContaining(
						"Endpoint bean 'nonJmxEndpoint' cannot support the extension bean 'nonJmxJmxEndpointExtension'"));
	}

	private Object getInvoker(JmxOperation operation) {
		return ReflectionTestUtils.getField(operation, "invoker");
	}

	private void assertJmxTestEndpoint(ExposableJmxEndpoint endpoint) {
		Map<String, JmxOperation> operationsByName = mapOperations(endpoint.getOperations());
		assertThat(operationsByName).containsOnlyKeys("getAll", "getSomething", "update", "deleteSomething");
		JmxOperation getAll = operationsByName.get("getAll");
		assertThat(getAll.getDescription()).isEqualTo("Get all the things");
		assertThat(getAll.getOutputType()).isEqualTo(Object.class);
		assertThat(getAll.getParameters()).isEmpty();
		JmxOperation getSomething = operationsByName.get("getSomething");
		assertThat(getSomething.getDescription()).isEqualTo("Get something based on a timeUnit");
		assertThat(getSomething.getOutputType()).isEqualTo(String.class);
		assertThat(getSomething.getParameters()).hasSize(1);
		hasDocumentedParameter(getSomething, 0, "unitMs", Long.class, "Number of milliseconds");
		JmxOperation update = operationsByName.get("update");
		assertThat(update.getDescription()).isEqualTo("Update something based on bar");
		assertThat(update.getOutputType()).isEqualTo(Void.TYPE);
		assertThat(update.getParameters()).hasSize(2);
		hasDocumentedParameter(update, 0, "foo", String.class, "Foo identifier");
		hasDocumentedParameter(update, 1, "bar", String.class, "Bar value");
		JmxOperation deleteSomething = operationsByName.get("deleteSomething");
		assertThat(deleteSomething.getDescription()).isEqualTo("Delete something based on a timeUnit");
		assertThat(deleteSomething.getOutputType()).isEqualTo(Void.TYPE);
		assertThat(deleteSomething.getParameters()).hasSize(1);
		hasDocumentedParameter(deleteSomething, 0, "unitMs", Long.class, "Number of milliseconds");
	}

	private void hasDocumentedParameter(JmxOperation operation, int index, String name, Class<?> type,
			String description) {
		assertThat(index).isLessThan(operation.getParameters().size());
		JmxOperationParameter parameter = operation.getParameters().get(index);
		assertThat(parameter.getName()).isEqualTo(name);
		assertThat(parameter.getType()).isEqualTo(type);
		assertThat(parameter.getDescription()).isEqualTo(description);
	}

	// FIXME rename
	private void hasDefaultParameter(JmxOperation operation, int index, Class<?> type) {
		JmxOperationParameter parameter = operation.getParameters().get(index);
		assertThat(parameter.getType()).isEqualTo(type);
	}

	private Map<EndpointId, ExposableJmxEndpoint> discover(JmxEndpointDiscoverer discoverer) {
		Map<EndpointId, ExposableJmxEndpoint> byId = new HashMap<>();
		discoverer.getEndpoints().forEach((endpoint) -> byId.put(endpoint.getEndpointId(), endpoint));
		return byId;
	}

	private Map<String, JmxOperation> mapOperations(Collection<JmxOperation> operations) {
		Map<String, JmxOperation> byName = new HashMap<>();
		operations.forEach((operation) -> byName.put(operation.getName(), operation));
		return byName;
	}

	private void load(Class<?> configuration, Consumer<JmxEndpointDiscoverer> consumer) {
		load(configuration, (id) -> null, consumer);
	}

	private void load(Class<?> configuration, Function<EndpointId, Long> timeToLive,
			Consumer<JmxEndpointDiscoverer> consumer) {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(configuration)) {
			ConversionServiceParameterValueMapper parameterMapper = new ConversionServiceParameterValueMapper(
					DefaultConversionService.getSharedInstance());
			JmxEndpointDiscoverer discoverer = new JmxEndpointDiscoverer(context, parameterMapper,
					Collections.singleton(new CachingOperationInvokerAdvisor(timeToLive)), Collections.emptyList());
			consumer.accept(discoverer);
		}
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
		TestJmxEndpoint testJmxEndpoint() {
			return new TestJmxEndpoint();
		}

		@Bean
		NonJmxEndpoint nonJmxEndpoint() {
			return new NonJmxEndpoint();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class OverriddenOperationJmxEndpointConfiguration {

		@Bean
		TestEndpoint testEndpoint() {
			return new TestEndpoint();
		}

		@Bean
		TestJmxEndpointExtension testJmxEndpointExtension() {
			return new TestJmxEndpointExtension();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class AdditionalOperationJmxEndpointConfiguration {

		@Bean
		TestEndpoint testEndpoint() {
			return new TestEndpoint();
		}

		@Bean
		AdditionalOperationJmxEndpointExtension additionalOperationJmxEndpointExtension() {
			return new AdditionalOperationJmxEndpointExtension();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class AdditionalClashingOperationsConfiguration {

		@Bean
		TestEndpoint testEndpoint() {
			return new TestEndpoint();
		}

		@Bean
		ClashingOperationsJmxEndpointExtension clashingOperationsJmxEndpointExtension() {
			return new ClashingOperationsJmxEndpointExtension();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ClashingJmxEndpointConfiguration {

		@Bean
		TestEndpoint testEndpoint() {
			return new TestEndpoint();
		}

		@Bean
		TestJmxEndpointExtension testExtensionOne() {
			return new TestJmxEndpointExtension();
		}

		@Bean
		TestJmxEndpointExtension testExtensionTwo() {
			return new TestJmxEndpointExtension();
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
	static class InvalidJmxExtensionConfiguration {

		@Bean
		NonJmxEndpoint nonJmxEndpoint() {
			return new NonJmxEndpoint();
		}

		@Bean
		NonJmxJmxEndpointExtension nonJmxJmxEndpointExtension() {
			return new NonJmxJmxEndpointExtension();
		}

	}

	@Endpoint(id = "test")
	static class TestEndpoint {

		@ReadOperation
		Object getAll() {
			return null;
		}

		@ReadOperation
		String getSomething(TimeUnit timeUnit) {
			return null;
		}

		@WriteOperation
		void update(String foo, String bar) {

		}

		@DeleteOperation
		void deleteSomething(TimeUnit timeUnit) {

		}

	}

	@JmxEndpoint(id = "jmx")
	static class TestJmxEndpoint {

		@ReadOperation
		Object getAll() {
			return null;
		}

	}

	@EndpointJmxExtension(endpoint = TestEndpoint.class)
	static class TestJmxEndpointExtension {

		@ManagedOperation(description = "Get all the things")
		@ReadOperation
		Object getAll() {
			return null;
		}

		@ReadOperation
		@ManagedOperation(description = "Get something based on a timeUnit")
		@ManagedOperationParameters({
				@ManagedOperationParameter(name = "unitMs", description = "Number of milliseconds") })
		String getSomething(Long timeUnit) {
			return null;
		}

		@WriteOperation
		@ManagedOperation(description = "Update something based on bar")
		@ManagedOperationParameters({ @ManagedOperationParameter(name = "foo", description = "Foo identifier"),
				@ManagedOperationParameter(name = "bar", description = "Bar value") })
		void update(String foo, String bar) {

		}

		@DeleteOperation
		@ManagedOperation(description = "Delete something based on a timeUnit")
		@ManagedOperationParameters({
				@ManagedOperationParameter(name = "unitMs", description = "Number of milliseconds") })
		void deleteSomething(Long timeUnit) {

		}

	}

	@EndpointJmxExtension(endpoint = TestEndpoint.class)
	static class AdditionalOperationJmxEndpointExtension {

		@ManagedOperation(description = "Get another thing")
		@ReadOperation
		Object getAnother() {
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
		Object getAll(String param) {
			return null;
		}

	}

	@EndpointJmxExtension(endpoint = TestEndpoint.class)
	static class ClashingOperationsJmxEndpointExtension {

		@ReadOperation
		Object getAll() {
			return null;
		}

		@ReadOperation
		Object getAll(String param) {
			return null;
		}

	}

	@WebEndpoint(id = "nonjmx")
	static class NonJmxEndpoint {

		@ReadOperation
		Object getData() {
			return null;
		}

	}

	@EndpointJmxExtension(endpoint = NonJmxEndpoint.class)
	static class NonJmxJmxEndpointExtension {

		@ReadOperation
		Object getSomething() {
			return null;
		}

	}

}
