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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.actuate.endpoint.EndpointDiscoverer;
import org.springframework.boot.actuate.endpoint.EndpointFilter;
import org.springframework.boot.actuate.endpoint.EndpointInfo;
import org.springframework.boot.actuate.endpoint.Operation;
import org.springframework.boot.actuate.endpoint.OperationInvoker;
import org.springframework.boot.actuate.endpoint.cache.CachingOperationInvoker;
import org.springframework.boot.actuate.endpoint.cache.CachingOperationInvokerAdvisor;
import org.springframework.boot.actuate.endpoint.convert.ConversionServiceParameterMapper;
import org.springframework.boot.actuate.endpoint.reflect.OperationMethodInfo;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;
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
	public void endpointInParentContextIsDiscovered() {
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
		Function<String, Long> timeToLive = (endpointId) -> 0L;
		load(TestEndpointConfiguration.class, (context) -> {
			Map<String, EndpointInfo<TestEndpointOperation>> endpoints = mapEndpoints(
					new TestAnnotationEndpointDiscoverer(context, timeToLive)
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
		Function<String, Long> timeToLive = (id) -> (id.equals("foo") ? 500L : 0L);
		load(TestEndpointConfiguration.class, (context) -> {
			Map<String, EndpointInfo<TestEndpointOperation>> endpoints = mapEndpoints(
					new TestAnnotationEndpointDiscoverer(context, timeToLive)
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
	public void endpointMainReadOperationIsCachedWithMatchingId() {
		Function<String, Long> timeToLive = (id) -> (id.equals("test") ? 500L : 0L);
		load(TestEndpointConfiguration.class, (context) -> {
			Map<String, EndpointInfo<TestEndpointOperation>> endpoints = mapEndpoints(
					new TestAnnotationEndpointDiscoverer(context, timeToLive)
							.discoverEndpoints());
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

	@Test
	public void specializedEndpointsAreFilteredFromRegular() throws Exception {
		load(TestEndpointsConfiguration.class, (context) -> {
			Map<String, EndpointInfo<TestEndpointOperation>> endpoints = mapEndpoints(
					new TestAnnotationEndpointDiscoverer(context).discoverEndpoints());
			assertThat(endpoints).containsOnlyKeys("test");
		});
	}

	@Test
	public void specializedEndpointsAreNotFilteredFromSpecialized() throws Exception {
		load(TestEndpointsConfiguration.class, (context) -> {
			Map<String, EndpointInfo<SpecializedTestEndpointOperation>> endpoints = mapEndpoints(
					new SpecializedTestAnnotationEndpointDiscoverer(context)
							.discoverEndpoints());
			assertThat(endpoints).containsOnlyKeys("test", "specialized");
		});
	}

	@Test
	public void extensionsAreApplied() throws Exception {
		load(TestEndpointsConfiguration.class, (context) -> {
			Map<String, EndpointInfo<SpecializedTestEndpointOperation>> endpoints = mapEndpoints(
					new SpecializedTestAnnotationEndpointDiscoverer(context)
							.discoverEndpoints());
			Map<Method, TestEndpointOperation> operations = mapOperations(
					endpoints.get("specialized"));
			assertThat(operations).containsKeys(
					ReflectionUtils.findMethod(SpecializedExtension.class, "getSpecial"));
		});
	}

	@Test
	public void filtersAreApplied() throws Exception {
		load(TestEndpointsConfiguration.class, (context) -> {
			EndpointFilter<SpecializedTestEndpointOperation> filter = (info,
					discoverer) -> !(info.getId().equals("specialized"));
			Map<String, EndpointInfo<SpecializedTestEndpointOperation>> endpoints = mapEndpoints(
					new SpecializedTestAnnotationEndpointDiscoverer(context,
							Collections.singleton(filter)).discoverEndpoints());
			assertThat(endpoints).containsOnlyKeys("test");
		});
	}

	private <T extends Operation> Map<String, EndpointInfo<T>> mapEndpoints(
			Collection<EndpointInfo<T>> endpoints) {
		Map<String, EndpointInfo<T>> endpointById = new LinkedHashMap<>();
		endpoints.forEach((endpoint) -> {
			EndpointInfo<T> existing = endpointById.put(endpoint.getId(), endpoint);
			if (existing != null) {
				throw new AssertionError(String.format(
						"Found endpoints with duplicate id '%s'", endpoint.getId()));
			}
		});
		return endpointById;
	}

	private Map<Method, TestEndpointOperation> mapOperations(
			EndpointInfo<? extends TestEndpointOperation> endpoint) {
		Map<Method, TestEndpointOperation> operationByMethod = new HashMap<>();
		endpoint.getOperations().forEach((operation) -> {
			Method method = operation.getMethodInfo().getMethod();
			Operation existing = operationByMethod.put(method, operation);
			if (existing != null) {
				throw new AssertionError(String.format(
						"Found endpoint with duplicate operation method '%s'", method));
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

	@SpecializedEndpoint(id = "specialized")
	static class SpecializedTestEndpoint {

		@ReadOperation
		public Object getAll() {
			return null;
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

	@Import({ TestEndpoint.class, SpecializedTestEndpoint.class,
			SpecializedExtension.class })
	static class TestEndpointsConfiguration {

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

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Endpoint
	@FilteredEndpoint(SpecializedEndpointFilter.class)
	public @interface SpecializedEndpoint {

		@AliasFor(annotation = Endpoint.class)
		String id();

	}

	@EndpointExtension(endpoint = SpecializedTestEndpoint.class, filter = SpecializedEndpointFilter.class)
	public static class SpecializedExtension {

		@ReadOperation
		public Object getSpecial() {
			return null;
		}

	}

	static class SpecializedEndpointFilter
			implements EndpointFilter<SpecializedTestEndpointOperation> {

		@Override
		public boolean match(EndpointInfo<SpecializedTestEndpointOperation> info,
				EndpointDiscoverer<SpecializedTestEndpointOperation> discoverer) {
			return discoverer instanceof SpecializedTestAnnotationEndpointDiscoverer;
		}

	}

	public static class TestAnnotationEndpointDiscoverer
			extends AnnotationEndpointDiscoverer<Method, TestEndpointOperation> {

		TestAnnotationEndpointDiscoverer(ApplicationContext applicationContext) {
			this(applicationContext, (id) -> null, null);
		}

		TestAnnotationEndpointDiscoverer(ApplicationContext applicationContext,
				Function<String, Long> timeToLive) {
			this(applicationContext, timeToLive, null);
		}

		TestAnnotationEndpointDiscoverer(ApplicationContext applicationContext,
				Function<String, Long> timeToLive,
				Collection<? extends EndpointFilter<TestEndpointOperation>> filters) {
			super(applicationContext, TestEndpointOperation::new,
					TestEndpointOperation::getMethod,
					new ConversionServiceParameterMapper(),
					Collections.singleton(new CachingOperationInvokerAdvisor(timeToLive)),
					filters);
		}

	}

	public static class SpecializedTestAnnotationEndpointDiscoverer extends
			AnnotationEndpointDiscoverer<Method, SpecializedTestEndpointOperation> {

		SpecializedTestAnnotationEndpointDiscoverer(
				ApplicationContext applicationContext) {
			this(applicationContext, (id) -> null, null);
		}

		SpecializedTestAnnotationEndpointDiscoverer(ApplicationContext applicationContext,
				Collection<? extends EndpointFilter<SpecializedTestEndpointOperation>> filters) {
			this(applicationContext, (id) -> null, filters);
		}

		SpecializedTestAnnotationEndpointDiscoverer(ApplicationContext applicationContext,
				Function<String, Long> timeToLive,
				Collection<? extends EndpointFilter<SpecializedTestEndpointOperation>> filters) {
			super(applicationContext, SpecializedTestEndpointOperation::new,
					SpecializedTestEndpointOperation::getMethod,
					new ConversionServiceParameterMapper(),
					Collections.singleton(new CachingOperationInvokerAdvisor(timeToLive)),
					filters);
		}

	}

	public static class TestEndpointOperation extends Operation {

		private final OperationMethodInfo methodInfo;

		public TestEndpointOperation(String endpointId, OperationMethodInfo methodInfo,
				Object target, OperationInvoker invoker) {
			super(methodInfo.getOperationType(), invoker, true);
			this.methodInfo = methodInfo;
		}

		public Method getMethod() {
			return this.methodInfo.getMethod();
		}

		public OperationMethodInfo getMethodInfo() {
			return this.methodInfo;
		}

	}

	public static class SpecializedTestEndpointOperation extends TestEndpointOperation {

		public SpecializedTestEndpointOperation(String endpointId,
				OperationMethodInfo methodInfo, Object target, OperationInvoker invoker) {
			super(endpointId, methodInfo, target, invoker);
		}

	}

}
