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

package org.springframework.boot.actuate.endpoint.jmx.annotation;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.actuate.endpoint.EndpointInfo;
import org.springframework.boot.actuate.endpoint.annotation.DeleteOperation;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.boot.actuate.endpoint.cache.CachingOperationInvoker;
import org.springframework.boot.actuate.endpoint.cache.CachingOperationInvokerAdvisor;
import org.springframework.boot.actuate.endpoint.convert.ConversionServiceParameterMapper;
import org.springframework.boot.actuate.endpoint.jmx.JmxEndpointOperationParameterInfo;
import org.springframework.boot.actuate.endpoint.jmx.JmxOperation;
import org.springframework.boot.actuate.endpoint.reflect.ReflectiveOperationInvoker;
import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpoint;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedOperationParameter;
import org.springframework.jmx.export.annotation.ManagedOperationParameters;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JmxAnnotationEndpointDiscoverer}.
 *
 * @author Stephane Nicoll
 */
public class JmxAnnotationEndpointDiscovererTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	@Test
	public void discoveryWorksWhenThereAreNoEndpoints() {
		load(EmptyConfiguration.class,
				(discoverer) -> assertThat(discoverer.discoverEndpoints()).isEmpty());
	}

	@Test
	public void standardEndpointIsDiscovered() {
		load(TestEndpoint.class, (discoverer) -> {
			Map<String, EndpointInfo<JmxOperation>> endpoints = discover(discoverer);
			assertThat(endpoints).containsOnlyKeys("test");
			Map<String, JmxOperation> operationByName = mapOperations(
					endpoints.get("test").getOperations());
			assertThat(operationByName).containsOnlyKeys("getAll", "getSomething",
					"update", "deleteSomething");
			JmxOperation getAll = operationByName.get("getAll");
			assertThat(getAll.getDescription())
					.isEqualTo("Invoke getAll for endpoint test");
			assertThat(getAll.getOutputType()).isEqualTo(Object.class);
			assertThat(getAll.getParameters()).isEmpty();
			assertThat(getAll.getInvoker())
					.isInstanceOf(ReflectiveOperationInvoker.class);
			JmxOperation getSomething = operationByName.get("getSomething");
			assertThat(getSomething.getDescription())
					.isEqualTo("Invoke getSomething for endpoint test");
			assertThat(getSomething.getOutputType()).isEqualTo(String.class);
			assertThat(getSomething.getParameters()).hasSize(1);
			hasDefaultParameter(getSomething, 0, String.class);
			JmxOperation update = operationByName.get("update");
			assertThat(update.getDescription())
					.isEqualTo("Invoke update for endpoint test");
			assertThat(update.getOutputType()).isEqualTo(Void.TYPE);
			assertThat(update.getParameters()).hasSize(2);
			hasDefaultParameter(update, 0, String.class);
			hasDefaultParameter(update, 1, String.class);
			JmxOperation deleteSomething = operationByName.get("deleteSomething");
			assertThat(deleteSomething.getDescription())
					.isEqualTo("Invoke deleteSomething for endpoint test");
			assertThat(deleteSomething.getOutputType()).isEqualTo(Void.TYPE);
			assertThat(deleteSomething.getParameters()).hasSize(1);
			hasDefaultParameter(deleteSomething, 0, String.class);
		});

	}

	@Test
	public void onlyJmxEndpointsAreDiscovered() {
		load(MultipleEndpointsConfiguration.class, (discoverer) -> {
			Map<String, EndpointInfo<JmxOperation>> endpoints = discover(discoverer);
			assertThat(endpoints).containsOnlyKeys("test", "jmx");
		});
	}

	@Test
	public void jmxExtensionMustHaveEndpoint() {
		load(TestJmxEndpointExtension.class, (discoverer) -> {
			this.thrown.expect(IllegalStateException.class);
			this.thrown.expectMessage("Invalid extension");
			this.thrown.expectMessage(TestJmxEndpointExtension.class.getName());
			this.thrown.expectMessage("no endpoint found");
			this.thrown.expectMessage(TestEndpoint.class.getName());
			discoverer.discoverEndpoints();
		});
	}

	@Test
	public void jmxEndpointOverridesStandardEndpoint() {
		load(OverriddenOperationJmxEndpointConfiguration.class, (discoverer) -> {
			Map<String, EndpointInfo<JmxOperation>> endpoints = discover(discoverer);
			assertThat(endpoints).containsOnlyKeys("test");
			assertJmxTestEndpoint(endpoints.get("test"));
		});
	}

	@Test
	public void jmxEndpointAddsExtraOperation() {
		load(AdditionalOperationJmxEndpointConfiguration.class, (discoverer) -> {
			Map<String, EndpointInfo<JmxOperation>> endpoints = discover(discoverer);
			assertThat(endpoints).containsOnlyKeys("test");
			Map<String, JmxOperation> operationByName = mapOperations(
					endpoints.get("test").getOperations());
			assertThat(operationByName).containsOnlyKeys("getAll", "getSomething",
					"update", "deleteSomething", "getAnother");
			JmxOperation getAnother = operationByName.get("getAnother");
			assertThat(getAnother.getDescription()).isEqualTo("Get another thing");
			assertThat(getAnother.getOutputType()).isEqualTo(Object.class);
			assertThat(getAnother.getParameters()).isEmpty();
		});
	}

	@Test
	public void endpointMainReadOperationIsCachedWithMatchingId() {
		load(TestEndpoint.class, (id) -> 500L, (discoverer) -> {
			Map<String, EndpointInfo<JmxOperation>> endpoints = discover(discoverer);
			assertThat(endpoints).containsOnlyKeys("test");
			Map<String, JmxOperation> operationByName = mapOperations(
					endpoints.get("test").getOperations());
			assertThat(operationByName).containsOnlyKeys("getAll", "getSomething",
					"update", "deleteSomething");
			JmxOperation getAll = operationByName.get("getAll");
			assertThat(getAll.getInvoker()).isInstanceOf(CachingOperationInvoker.class);
			assertThat(((CachingOperationInvoker) getAll.getInvoker()).getTimeToLive())
					.isEqualTo(500);
		});
	}

	@Test
	public void extraReadOperationsAreCached() {
		load(AdditionalOperationJmxEndpointConfiguration.class, (id) -> 500L,
				(discoverer) -> {
					Map<String, EndpointInfo<JmxOperation>> endpoints = discover(
							discoverer);
					assertThat(endpoints).containsOnlyKeys("test");
					Map<String, JmxOperation> operationByName = mapOperations(
							endpoints.get("test").getOperations());
					assertThat(operationByName).containsOnlyKeys("getAll", "getSomething",
							"update", "deleteSomething", "getAnother");
					JmxOperation getAll = operationByName.get("getAll");
					assertThat(getAll.getInvoker())
							.isInstanceOf(CachingOperationInvoker.class);
					assertThat(((CachingOperationInvoker) getAll.getInvoker())
							.getTimeToLive()).isEqualTo(500);
					JmxOperation getAnother = operationByName.get("getAnother");
					assertThat(getAnother.getInvoker())
							.isInstanceOf(CachingOperationInvoker.class);
					assertThat(((CachingOperationInvoker) getAnother.getInvoker())
							.getTimeToLive()).isEqualTo(500);
				});
	}

	@Test
	public void discoveryFailsWhenTwoExtensionsHaveTheSameEndpointType() {
		load(ClashingJmxEndpointConfiguration.class, (discoverer) -> {
			this.thrown.expect(IllegalStateException.class);
			this.thrown.expectMessage("Found two extensions for the same endpoint");
			this.thrown.expectMessage(TestEndpoint.class.getName());
			this.thrown.expectMessage(TestJmxEndpointExtension.class.getName());
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
	public void discoveryFailsWhenEndpointHasTwoOperationsWithTheSameName() {
		load(ClashingOperationsEndpoint.class, (discoverer) -> {
			this.thrown.expect(IllegalStateException.class);
			this.thrown.expectMessage("Found multiple JMX operations with the same name");
			this.thrown.expectMessage("getAll");
			discoverer.discoverEndpoints();
		});
	}

	@Test
	public void discoveryFailsWhenExtensionHasTwoOperationsWithTheSameName() {
		load(AdditionalClashingOperationsConfiguration.class, (discoverer) -> {
			this.thrown.expect(IllegalStateException.class);
			this.thrown.expectMessage("Found multiple JMX operations with the same name");
			this.thrown.expectMessage("getAll");
			discoverer.discoverEndpoints();
		});
	}

	@Test
	public void discoveryFailsWhenExtensionIsNotCompatibleWithTheEndpointType() {
		load(InvalidJmxExtensionConfiguration.class, (discoverer) -> {
			this.thrown.expect(IllegalStateException.class);
			this.thrown.expectMessage("Invalid extension");
			this.thrown.expectMessage(NonJmxJmxEndpointExtension.class.getName());
			this.thrown.expectMessage(NonJmxEndpoint.class.getName());
			discoverer.discoverEndpoints();
		});
	}

	private void assertJmxTestEndpoint(EndpointInfo<JmxOperation> endpoint) {
		Map<String, JmxOperation> operationByName = mapOperations(
				endpoint.getOperations());
		assertThat(operationByName).containsOnlyKeys("getAll", "getSomething", "update",
				"deleteSomething");
		JmxOperation getAll = operationByName.get("getAll");
		assertThat(getAll.getDescription()).isEqualTo("Get all the things");
		assertThat(getAll.getOutputType()).isEqualTo(Object.class);
		assertThat(getAll.getParameters()).isEmpty();
		JmxOperation getSomething = operationByName.get("getSomething");
		assertThat(getSomething.getDescription())
				.isEqualTo("Get something based on a timeUnit");
		assertThat(getSomething.getOutputType()).isEqualTo(String.class);
		assertThat(getSomething.getParameters()).hasSize(1);
		hasDocumentedParameter(getSomething, 0, "unitMs", Long.class,
				"Number of milliseconds");
		JmxOperation update = operationByName.get("update");
		assertThat(update.getDescription()).isEqualTo("Update something based on bar");
		assertThat(update.getOutputType()).isEqualTo(Void.TYPE);
		assertThat(update.getParameters()).hasSize(2);
		hasDocumentedParameter(update, 0, "foo", String.class, "Foo identifier");
		hasDocumentedParameter(update, 1, "bar", String.class, "Bar value");
		JmxOperation deleteSomething = operationByName.get("deleteSomething");
		assertThat(deleteSomething.getDescription())
				.isEqualTo("Delete something based on a timeUnit");
		assertThat(deleteSomething.getOutputType()).isEqualTo(Void.TYPE);
		assertThat(deleteSomething.getParameters()).hasSize(1);
		hasDocumentedParameter(deleteSomething, 0, "unitMs", Long.class,
				"Number of milliseconds");
	}

	private void hasDefaultParameter(JmxOperation operation, int index, Class<?> type) {
		assertThat(index).isLessThan(operation.getParameters().size());
		JmxEndpointOperationParameterInfo parameter = operation.getParameters()
				.get(index);
		assertThat(parameter.getType()).isEqualTo(type);
		assertThat(parameter.getDescription()).isNull();
	}

	private void hasDocumentedParameter(JmxOperation operation, int index, String name,
			Class<?> type, String description) {
		assertThat(index).isLessThan(operation.getParameters().size());
		JmxEndpointOperationParameterInfo parameter = operation.getParameters()
				.get(index);
		assertThat(parameter.getName()).isEqualTo(name);
		assertThat(parameter.getType()).isEqualTo(type);
		assertThat(parameter.getDescription()).isEqualTo(description);
	}

	private Map<String, EndpointInfo<JmxOperation>> discover(
			JmxAnnotationEndpointDiscoverer discoverer) {
		Map<String, EndpointInfo<JmxOperation>> endpointsById = new HashMap<>();
		discoverer.discoverEndpoints()
				.forEach((endpoint) -> endpointsById.put(endpoint.getId(), endpoint));
		return endpointsById;
	}

	private Map<String, JmxOperation> mapOperations(Collection<JmxOperation> operations) {
		Map<String, JmxOperation> operationByName = new HashMap<>();
		operations.forEach((operation) -> operationByName
				.put(operation.getOperationName(), operation));
		return operationByName;
	}

	private void load(Class<?> configuration,
			Consumer<JmxAnnotationEndpointDiscoverer> consumer) {
		load(configuration, (id) -> null, consumer);
	}

	private void load(Class<?> configuration, Function<String, Long> timeToLive,
			Consumer<JmxAnnotationEndpointDiscoverer> consumer) {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				configuration)) {
			ConversionServiceParameterMapper parameterMapper = new ConversionServiceParameterMapper(
					DefaultConversionService.getSharedInstance());
			JmxAnnotationEndpointDiscoverer discoverer = new JmxAnnotationEndpointDiscoverer(
					context, parameterMapper,
					Collections.singleton(new CachingOperationInvokerAdvisor(timeToLive)),
					null);
			consumer.accept(discoverer);
		}
	}

	@Endpoint(id = "test")
	private static class TestEndpoint {

		@ReadOperation
		public Object getAll() {
			return null;
		}

		@ReadOperation
		public String getSomething(TimeUnit timeUnit) {
			return null;
		}

		@WriteOperation
		public void update(String foo, String bar) {

		}

		@DeleteOperation
		public void deleteSomething(TimeUnit timeUnit) {

		}

	}

	@JmxEndpoint(id = "jmx")
	private static class TestJmxEndpoint {

		@ReadOperation
		public Object getAll() {
			return null;
		}

	}

	@EndpointJmxExtension(endpoint = TestEndpoint.class)
	private static class TestJmxEndpointExtension {

		@ManagedOperation(description = "Get all the things")
		@ReadOperation
		public Object getAll() {
			return null;
		}

		@ReadOperation
		@ManagedOperation(description = "Get something based on a timeUnit")
		@ManagedOperationParameters({
				@ManagedOperationParameter(name = "unitMs", description = "Number of milliseconds") })
		public String getSomething(Long timeUnit) {
			return null;
		}

		@WriteOperation
		@ManagedOperation(description = "Update something based on bar")
		@ManagedOperationParameters({
				@ManagedOperationParameter(name = "foo", description = "Foo identifier"),
				@ManagedOperationParameter(name = "bar", description = "Bar value") })
		public void update(String foo, String bar) {

		}

		@DeleteOperation
		@ManagedOperation(description = "Delete something based on a timeUnit")
		@ManagedOperationParameters({
				@ManagedOperationParameter(name = "unitMs", description = "Number of milliseconds") })
		public void deleteSomething(Long timeUnit) {

		}

	}

	@EndpointJmxExtension(endpoint = TestEndpoint.class)
	private static class AdditionalOperationJmxEndpointExtension {

		@ManagedOperation(description = "Get another thing")
		@ReadOperation
		public Object getAnother() {
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
		public Object getAll(String param) {
			return null;
		}

	}

	@EndpointJmxExtension(endpoint = TestEndpoint.class)
	static class ClashingOperationsJmxEndpointExtension {

		@ReadOperation
		public Object getAll() {
			return null;
		}

		@ReadOperation
		public Object getAll(String param) {
			return null;
		}

	}

	@WebEndpoint(id = "nonjmx")
	private static class NonJmxEndpoint {

		@ReadOperation
		public Object getData() {
			return null;
		}

	}

	@EndpointJmxExtension(endpoint = NonJmxEndpoint.class)
	private static class NonJmxJmxEndpointExtension {

		@ReadOperation
		public Object getSomething() {
			return null;
		}

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
		public TestJmxEndpoint testJmxEndpoint() {
			return new TestJmxEndpoint();
		}

		@Bean
		public NonJmxEndpoint nonJmxEndpoint() {
			return new NonJmxEndpoint();
		}

	}

	@Configuration
	static class OverriddenOperationJmxEndpointConfiguration {

		@Bean
		public TestEndpoint testEndpoint() {
			return new TestEndpoint();
		}

		@Bean
		public TestJmxEndpointExtension testJmxEndpointExtension() {
			return new TestJmxEndpointExtension();
		}

	}

	@Configuration
	static class AdditionalOperationJmxEndpointConfiguration {

		@Bean
		public TestEndpoint testEndpoint() {
			return new TestEndpoint();
		}

		@Bean
		public AdditionalOperationJmxEndpointExtension additionalOperationJmxEndpointExtension() {
			return new AdditionalOperationJmxEndpointExtension();
		}

	}

	@Configuration
	static class AdditionalClashingOperationsConfiguration {

		@Bean
		public TestEndpoint testEndpoint() {
			return new TestEndpoint();
		}

		@Bean
		public ClashingOperationsJmxEndpointExtension clashingOperationsJmxEndpointExtension() {
			return new ClashingOperationsJmxEndpointExtension();
		}

	}

	@Configuration
	static class ClashingJmxEndpointConfiguration {

		@Bean
		public TestEndpoint testEndpoint() {
			return new TestEndpoint();
		}

		@Bean
		public TestJmxEndpointExtension testExtensionOne() {
			return new TestJmxEndpointExtension();
		}

		@Bean
		public TestJmxEndpointExtension testExtensionTwo() {
			return new TestJmxEndpointExtension();
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
	static class InvalidJmxExtensionConfiguration {

		@Bean
		public NonJmxEndpoint nonJmxEndpoint() {
			return new NonJmxEndpoint();
		}

		@Bean
		public NonJmxJmxEndpointExtension nonJmxJmxEndpointExtension() {
			return new NonJmxJmxEndpointExtension();
		}

	}

}
