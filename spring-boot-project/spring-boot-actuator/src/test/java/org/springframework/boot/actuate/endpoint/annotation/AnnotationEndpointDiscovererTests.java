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

package org.springframework.boot.actuate.endpoint.annotation;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.actuate.endpoint.EndpointInfo;
import org.springframework.boot.actuate.endpoint.Operation;
import org.springframework.boot.actuate.endpoint.OperationInvoker;
import org.springframework.boot.actuate.endpoint.OperationType;
import org.springframework.boot.actuate.endpoint.cache.CachingConfiguration;
import org.springframework.boot.actuate.endpoint.cache.CachingConfigurationFactory;
import org.springframework.boot.actuate.endpoint.cache.CachingOperationInvoker;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AnnotationEndpointDiscoverer}.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 */
public class AnnotationEndpointDiscovererTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	@Test
	public void discoverWorksWhenThereAreNoEndpoints() {
		load(EmptyConfiguration.class,
				(context) -> assertThat(new TestAnnotationEndpointDiscoverer(context)
						.discoverEndpoints().isEmpty()));
	}

	@Test
	public void endpointIsDiscovered() {
		load(TestEndpointConfiguration.class, hasTestEndpoint());
	}

	@Test
	public void endpointIsInParentContextIsDiscovered() {
		AnnotationConfigApplicationContext parent = new AnnotationConfigApplicationContext(
				TestEndpointConfiguration.class);
		loadWithParent(parent, EmptyConfiguration.class, hasTestEndpoint());
	}

	private Consumer<AnnotationConfigApplicationContext> hasTestEndpoint() {
		return (context) -> {
			Map<String, EndpointInfo<TestEndpointOperation>> endpoints = mapEndpoints(
					new TestAnnotationEndpointDiscoverer(context).discoverEndpoints());
			assertThat(endpoints).containsOnlyKeys("test");
			Map<Method, TestEndpointOperation> operations = mapOperations(
					endpoints.get("test"));
			assertThat(operations).hasSize(4);
			assertThat(operations).containsKeys(
					ReflectionUtils.findMethod(TestEndpoint.class, "getAll"),
					ReflectionUtils.findMethod(TestEndpoint.class, "getOne",
							String.class),
					ReflectionUtils.findMethod(TestEndpoint.class, "update", String.class,
							String.class),
					ReflectionUtils.findMethod(TestEndpoint.class, "deleteOne",
							String.class));
		};
	}

	@Test
	public void subclassedEndpointIsDiscovered() {
		load(TestEndpointSubclassConfiguration.class, (context) -> {
			Map<String, EndpointInfo<TestEndpointOperation>> endpoints = mapEndpoints(
					new TestAnnotationEndpointDiscoverer(context).discoverEndpoints());
			assertThat(endpoints).containsOnlyKeys("test");
			Map<Method, TestEndpointOperation> operations = mapOperations(
					endpoints.get("test"));
			assertThat(operations).hasSize(5);
			assertThat(operations).containsKeys(
					ReflectionUtils.findMethod(TestEndpoint.class, "getAll"),
					ReflectionUtils.findMethod(TestEndpoint.class, "getOne",
							String.class),
					ReflectionUtils.findMethod(TestEndpoint.class, "update", String.class,
							String.class),
					ReflectionUtils.findMethod(TestEndpoint.class, "deleteOne",
							String.class),
					ReflectionUtils.findMethod(TestEndpointSubclass.class,
							"updateWithMoreArguments", String.class, String.class,
							String.class));
		});
	}

	@Test
	public void discoveryFailsWhenTwoEndpointsHaveTheSameId() {
		load(ClashingEndpointConfiguration.class, (context) -> {
			this.thrown.expect(IllegalStateException.class);
			this.thrown.expectMessage("Found two endpoints with the id 'test': ");
			new TestAnnotationEndpointDiscoverer(context).discoverEndpoints();
		});
	}

	@Test
	public void endpointMainReadOperationIsNotCachedWithTtlSetToZero() {
		load(TestEndpointConfiguration.class, (context) -> {
			Map<String, EndpointInfo<TestEndpointOperation>> endpoints = mapEndpoints(
					new TestAnnotationEndpointDiscoverer(context,
							(endpointId) -> new CachingConfiguration(0))
									.discoverEndpoints());
			assertThat(endpoints).containsOnlyKeys("test");
			Map<Method, TestEndpointOperation> operations = mapOperations(
					endpoints.get("test"));
			assertThat(operations).hasSize(4);
			operations.values().forEach(operation -> assertThat(operation.getInvoker())
					.isNotInstanceOf(CachingOperationInvoker.class));
		});
	}

	@Test
	public void endpointMainReadOperationIsNotCachedWithNonMatchingId() {
		CachingConfigurationFactory cachingConfigurationFactory = (
				endpointId) -> (endpointId.equals("foo") ? new CachingConfiguration(500)
						: new CachingConfiguration(0));
		load(TestEndpointConfiguration.class, (context) -> {
			Map<String, EndpointInfo<TestEndpointOperation>> endpoints = mapEndpoints(
					new TestAnnotationEndpointDiscoverer(context,
							cachingConfigurationFactory).discoverEndpoints());
			assertThat(endpoints).containsOnlyKeys("test");
			Map<Method, TestEndpointOperation> operations = mapOperations(
					endpoints.get("test"));
			assertThat(operations).hasSize(4);
			operations.values().forEach(operation -> assertThat(operation.getInvoker())
					.isNotInstanceOf(CachingOperationInvoker.class));
		});
	}

	@Test
	public void endpointMainReadOperationIsCachedWithMatchingId() {
		CachingConfigurationFactory cachingConfigurationFactory = (
				endpointId) -> (endpointId.equals("test") ? new CachingConfiguration(500)
						: new CachingConfiguration(0));
		load(TestEndpointConfiguration.class, (context) -> {
			Map<String, EndpointInfo<TestEndpointOperation>> endpoints = mapEndpoints(
					new TestAnnotationEndpointDiscoverer(context,
							cachingConfigurationFactory).discoverEndpoints());
			assertThat(endpoints).containsOnlyKeys("test");
			Map<Method, TestEndpointOperation> operations = mapOperations(
					endpoints.get("test"));
			OperationInvoker getAllOperationInvoker = operations
					.get(ReflectionUtils.findMethod(TestEndpoint.class, "getAll"))
					.getInvoker();
			assertThat(getAllOperationInvoker)
					.isInstanceOf(CachingOperationInvoker.class);
			assertThat(((CachingOperationInvoker) getAllOperationInvoker).getTimeToLive())
					.isEqualTo(500);
			assertThat(operations.get(ReflectionUtils.findMethod(TestEndpoint.class,
					"getOne", String.class)).getInvoker())
							.isNotInstanceOf(CachingOperationInvoker.class);
			assertThat(operations.get(ReflectionUtils.findMethod(TestEndpoint.class,
					"update", String.class, String.class)).getInvoker())
							.isNotInstanceOf(CachingOperationInvoker.class);
		});
	}

	private Map<String, EndpointInfo<TestEndpointOperation>> mapEndpoints(
			Collection<EndpointInfo<TestEndpointOperation>> endpoints) {
		Map<String, EndpointInfo<TestEndpointOperation>> endpointById = new LinkedHashMap<>();
		endpoints.forEach((endpoint) -> {
			EndpointInfo<TestEndpointOperation> existing = endpointById
					.put(endpoint.getId(), endpoint);
			if (existing != null) {
				throw new AssertionError(String.format(
						"Found endpoints with duplicate id '%s'", endpoint.getId()));
			}
		});
		return endpointById;
	}

	private Map<Method, TestEndpointOperation> mapOperations(
			EndpointInfo<TestEndpointOperation> endpoint) {
		Map<Method, TestEndpointOperation> operationByMethod = new HashMap<>();
		endpoint.getOperations().forEach((operation) -> {
			Operation existing = operationByMethod.put(operation.getOperationMethod(),
					operation);
			if (existing != null) {
				throw new AssertionError(String.format(
						"Found endpoint with duplicate operation method '%s'",
						operation.getOperationMethod()));
			}
		});
		return operationByMethod;
	}

	private void load(Class<?> configuration,
			Consumer<AnnotationConfigApplicationContext> consumer) {
		doLoad(null, configuration, consumer);
	}

	private void loadWithParent(ApplicationContext parent, Class<?> configuration,
			Consumer<AnnotationConfigApplicationContext> consumer) {
		doLoad(parent, configuration, consumer);
	}

	private void doLoad(ApplicationContext parent, Class<?> configuration,
			Consumer<AnnotationConfigApplicationContext> consumer) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		if (parent != null) {
			context.setParent(parent);
		}
		context.register(configuration);
		context.refresh();
		try {
			consumer.accept(context);
		}
		finally {
			context.close();
		}
	}

	@Configuration
	static class EmptyConfiguration {

	}

	@Endpoint(id = "test")
	static class TestEndpoint {

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

		@DeleteOperation
		public void deleteOne(@Selector String id) {

		}

		public void someOtherMethod() {

		}

	}

	static class TestEndpointSubclass extends TestEndpoint {

		@WriteOperation
		public void updateWithMoreArguments(String foo, String bar, String baz) {

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
	static class TestEndpointSubclassConfiguration {

		@Bean
		public TestEndpointSubclass testEndpointSubclass() {
			return new TestEndpointSubclass();
		}

	}

	@Configuration
	static class ClashingEndpointConfiguration {

		@Bean
		public TestEndpoint testEndpointTwo() {
			return new TestEndpoint();
		}

		@Bean
		public TestEndpoint testEndpointOne() {
			return new TestEndpoint();
		}
	}

	private static final class TestEndpointOperation extends Operation {

		private final Method operationMethod;

		private TestEndpointOperation(OperationType type,
				OperationInvoker operationInvoker, Method operationMethod) {
			super(type, operationInvoker, true);
			this.operationMethod = operationMethod;
		}

		private Method getOperationMethod() {
			return this.operationMethod;
		}

	}

	private static class TestAnnotationEndpointDiscoverer
			extends AnnotationEndpointDiscoverer<TestEndpointOperation, Method> {

		TestAnnotationEndpointDiscoverer(ApplicationContext applicationContext,
				CachingConfigurationFactory cachingConfigurationFactory) {
			super(applicationContext, endpointOperationFactory(),
					TestEndpointOperation::getOperationMethod,
					cachingConfigurationFactory);
		}

		TestAnnotationEndpointDiscoverer(ApplicationContext applicationContext) {
			this(applicationContext, (id) -> null);
		}

		@Override
		public Collection<EndpointInfo<TestEndpointOperation>> discoverEndpoints() {
			return discoverEndpoints(null, null).stream()
					.map(EndpointInfoDescriptor::getEndpointInfo)
					.collect(Collectors.toList());
		}

		private static EndpointOperationFactory<TestEndpointOperation> endpointOperationFactory() {
			return new EndpointOperationFactory<TestEndpointOperation>() {

				@Override
				public TestEndpointOperation createOperation(String endpointId,
						AnnotationAttributes operationAttributes, Object target,
						Method operationMethod, OperationType operationType,
						long timeToLive) {
					return new TestEndpointOperation(operationType,
							createOperationInvoker(timeToLive), operationMethod);
				}

				private OperationInvoker createOperationInvoker(long timeToLive) {
					OperationInvoker invoker = (arguments) -> null;
					if (timeToLive > 0) {
						return new CachingOperationInvoker(invoker, timeToLive);
					}
					else {
						return invoker;
					}
				}
			};
		}

	}

}
