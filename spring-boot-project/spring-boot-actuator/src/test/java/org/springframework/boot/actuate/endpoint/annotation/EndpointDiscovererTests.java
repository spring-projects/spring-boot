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

package org.springframework.boot.actuate.endpoint.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.actuate.endpoint.EndpointFilter;
import org.springframework.boot.actuate.endpoint.ExposableEndpoint;
import org.springframework.boot.actuate.endpoint.Operation;
import org.springframework.boot.actuate.endpoint.invoke.OperationInvoker;
import org.springframework.boot.actuate.endpoint.invoke.OperationInvokerAdvisor;
import org.springframework.boot.actuate.endpoint.invoke.ParameterValueMapper;
import org.springframework.boot.actuate.endpoint.invoke.convert.ConversionServiceParameterValueMapper;
import org.springframework.boot.actuate.endpoint.invoker.cache.CachingOperationInvoker;
import org.springframework.boot.actuate.endpoint.invoker.cache.CachingOperationInvokerAdvisor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link EndpointDiscoverer}.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
public class EndpointDiscovererTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	@Test
	public void createWhenApplicationContextIsNullShouldThrowException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("ApplicationContext must not be null");
		new TestEndpointDiscoverer(null, mock(ParameterValueMapper.class),
				Collections.emptyList(), Collections.emptyList());
	}

	@Test
	public void createWhenParameterValueMapperIsNullShouldThrowException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("ParameterValueMapper must not be null");
		new TestEndpointDiscoverer(mock(ApplicationContext.class), null,
				Collections.emptyList(), Collections.emptyList());
	}

	@Test
	public void createWhenInvokerAdvisorsIsNullShouldThrowException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("InvokerAdvisors must not be null");
		new TestEndpointDiscoverer(mock(ApplicationContext.class),
				mock(ParameterValueMapper.class), null, Collections.emptyList());
	}

	@Test
	public void createWhenFiltersIsNullShouldThrowException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Filters must not be null");
		new TestEndpointDiscoverer(mock(ApplicationContext.class),
				mock(ParameterValueMapper.class), Collections.emptyList(), null);
	}

	@Test
	public void getEndpointsWhenNoEndpointBeansShouldReturnEmptyCollection() {
		load(EmptyConfiguration.class, (context) -> {
			TestEndpointDiscoverer discoverer = new TestEndpointDiscoverer(context);
			Collection<TestExposableEndpoint> endpoints = discoverer.getEndpoints();
			assertThat(endpoints).isEmpty();
		});
	}

	@Test
	public void getEndpointsWhenHasEndpointShouldReturnEndpoint() {
		load(TestEndpointConfiguration.class, this::hasTestEndpoint);
	}

	@Test
	public void getEndpointsWhenHasEndpointInParentContextShouldReturnEndpoint() {
		AnnotationConfigApplicationContext parent = new AnnotationConfigApplicationContext(
				TestEndpointConfiguration.class);
		loadWithParent(parent, EmptyConfiguration.class, this::hasTestEndpoint);
	}

	@Test
	public void getEndpointsWhenHasSubclassedEndpointShouldReturnEndpoint() {
		load(TestEndpointSubclassConfiguration.class, (context) -> {
			TestEndpointDiscoverer discoverer = new TestEndpointDiscoverer(context);
			Map<String, TestExposableEndpoint> endpoints = mapEndpoints(
					discoverer.getEndpoints());
			assertThat(endpoints).containsOnlyKeys("test");
			Map<Method, TestOperation> operations = mapOperations(endpoints.get("test"));
			assertThat(operations).hasSize(5);
			assertThat(operations).containsKeys(testEndpointMethods());
			assertThat(operations).containsKeys(ReflectionUtils.findMethod(
					TestEndpointSubclass.class, "updateWithMoreArguments", String.class,
					String.class, String.class));
		});
	}

	@Test
	public void getEndpointsWhenTwoEndpointsHaveTheSameIdShouldThrowException() {
		load(ClashingEndpointConfiguration.class, (context) -> {
			this.thrown.expect(IllegalStateException.class);
			this.thrown.expectMessage("Found two endpoints with the id 'test': ");
			new TestEndpointDiscoverer(context).getEndpoints();
		});
	}

	@Test
	public void getEndpointsWhenTtlSetToZeroShouldNotCacheInvokeCalls() {
		load(TestEndpointConfiguration.class, (context) -> {
			TestEndpointDiscoverer discoverer = new TestEndpointDiscoverer(context,
					(endpointId) -> 0L);
			Map<String, TestExposableEndpoint> endpoints = mapEndpoints(
					discoverer.getEndpoints());
			assertThat(endpoints).containsOnlyKeys("test");
			Map<Method, TestOperation> operations = mapOperations(endpoints.get("test"));
			operations.values().forEach((operation) -> assertThat(operation.getInvoker())
					.isNotInstanceOf(CachingOperationInvoker.class));
		});
	}

	@Test
	public void getEndpointsWhenTtlSetByIdAndIdDoesNotMatchShouldNotCacheInvokeCalls() {
		load(TestEndpointConfiguration.class, (context) -> {
			TestEndpointDiscoverer discoverer = new TestEndpointDiscoverer(context,
					(endpointId) -> (endpointId.equals("foo") ? 500L : 0L));
			Map<String, TestExposableEndpoint> endpoints = mapEndpoints(
					discoverer.getEndpoints());
			assertThat(endpoints).containsOnlyKeys("test");
			Map<Method, TestOperation> operations = mapOperations(endpoints.get("test"));
			operations.values().forEach((operation) -> assertThat(operation.getInvoker())
					.isNotInstanceOf(CachingOperationInvoker.class));
		});
	}

	@Test
	public void getEndpointsWhenTtlSetByIdAndIdMatchesShouldCacheInvokeCalls() {
		load(TestEndpointConfiguration.class, (context) -> {
			TestEndpointDiscoverer discoverer = new TestEndpointDiscoverer(context,
					(endpointId) -> (endpointId.equals("test") ? 500L : 0L));
			Map<String, TestExposableEndpoint> endpoints = mapEndpoints(
					discoverer.getEndpoints());
			assertThat(endpoints).containsOnlyKeys("test");
			Map<Method, TestOperation> operations = mapOperations(endpoints.get("test"));
			TestOperation getAll = operations.get(findTestEndpointMethod("getAll"));
			TestOperation getOne = operations
					.get(findTestEndpointMethod("getOne", String.class));
			TestOperation update = operations.get(ReflectionUtils.findMethod(
					TestEndpoint.class, "update", String.class, String.class));
			assertThat(((CachingOperationInvoker) getAll.getInvoker()).getTimeToLive())
					.isEqualTo(500);
			assertThat(getOne.getInvoker())
					.isNotInstanceOf(CachingOperationInvoker.class);
			assertThat(update.getInvoker())
					.isNotInstanceOf(CachingOperationInvoker.class);
		});
	}

	@Test
	public void getEndpointsWhenHasSpecializedFiltersInNonSpecializedDiscovererShouldFilterEndpoints() {
		load(SpecializedEndpointsConfiguration.class, (context) -> {
			TestEndpointDiscoverer discoverer = new TestEndpointDiscoverer(context);
			Map<String, TestExposableEndpoint> endpoints = mapEndpoints(
					discoverer.getEndpoints());
			assertThat(endpoints).containsOnlyKeys("test");
		});
	}

	@Test
	public void getEndpointsWhenHasSpecializedFiltersInSpecializedDiscovererShouldNotFilterEndpoints() {
		load(SpecializedEndpointsConfiguration.class, (context) -> {
			SpecializedEndpointDiscoverer discoverer = new SpecializedEndpointDiscoverer(
					context);
			Map<String, SpecializedExposableEndpoint> endpoints = mapEndpoints(
					discoverer.getEndpoints());
			assertThat(endpoints).containsOnlyKeys("test", "specialized");
		});
	}

	@Test
	public void getEndpointsShouldApplyExtensions() {
		load(SpecializedEndpointsConfiguration.class, (context) -> {
			SpecializedEndpointDiscoverer discoverer = new SpecializedEndpointDiscoverer(
					context);
			Map<String, SpecializedExposableEndpoint> endpoints = mapEndpoints(
					discoverer.getEndpoints());
			Map<Method, SpecializedOperation> operations = mapOperations(
					endpoints.get("specialized"));
			assertThat(operations).containsKeys(
					ReflectionUtils.findMethod(SpecializedExtension.class, "getSpecial"));

		});
	}

	@Test
	public void getEndpointShouldFindParentExtension() {
		load(SubSpecializedEndpointsConfiguration.class, (context) -> {
			SpecializedEndpointDiscoverer discoverer = new SpecializedEndpointDiscoverer(
					context);
			Map<String, SpecializedExposableEndpoint> endpoints = mapEndpoints(
					discoverer.getEndpoints());
			Map<Method, SpecializedOperation> operations = mapOperations(
					endpoints.get("specialized"));
			assertThat(operations).containsKeys(
					ReflectionUtils.findMethod(SpecializedTestEndpoint.class, "getAll"));
			assertThat(operations).containsKeys(ReflectionUtils.findMethod(
					SubSpecializedTestEndpoint.class, "getSpecialOne", String.class));
			assertThat(operations).containsKeys(
					ReflectionUtils.findMethod(SpecializedExtension.class, "getSpecial"));
			assertThat(operations).hasSize(3);
		});
	}

	@Test
	public void getEndpointsShouldApplyFilters() {
		load(SpecializedEndpointsConfiguration.class, (context) -> {
			EndpointFilter<SpecializedExposableEndpoint> filter = (endpoint) -> {
				String id = endpoint.getId();
				return !id.equals("specialized");
			};
			SpecializedEndpointDiscoverer discoverer = new SpecializedEndpointDiscoverer(
					context, Collections.singleton(filter));
			Map<String, SpecializedExposableEndpoint> endpoints = mapEndpoints(
					discoverer.getEndpoints());
			assertThat(endpoints).containsOnlyKeys("test");
		});
	}

	private void hasTestEndpoint(AnnotationConfigApplicationContext context) {
		TestEndpointDiscoverer discoverer = new TestEndpointDiscoverer(context);
		Map<String, TestExposableEndpoint> endpoints = mapEndpoints(
				discoverer.getEndpoints());
		assertThat(endpoints).containsOnlyKeys("test");
		Map<Method, TestOperation> operations = mapOperations(endpoints.get("test"));
		assertThat(operations).hasSize(4);
		assertThat(operations).containsKeys();
	}

	private Method[] testEndpointMethods() {
		List<Method> methods = new ArrayList<>();
		methods.add(findTestEndpointMethod("getAll"));
		methods.add(findTestEndpointMethod("getOne", String.class));
		methods.add(findTestEndpointMethod("update", String.class, String.class));
		methods.add(findTestEndpointMethod("deleteOne", String.class));
		return methods.toArray(new Method[0]);
	}

	private Method findTestEndpointMethod(String name, Class<?>... paramTypes) {
		return ReflectionUtils.findMethod(TestEndpoint.class, name, paramTypes);
	}

	private <E extends ExposableEndpoint<?>> Map<String, E> mapEndpoints(
			Collection<E> endpoints) {
		Map<String, E> byId = new LinkedHashMap<>();
		endpoints.forEach((endpoint) -> {
			E existing = byId.put(endpoint.getId(), endpoint);
			if (existing != null) {
				throw new AssertionError(String.format(
						"Found endpoints with duplicate id '%s'", endpoint.getId()));
			}
		});
		return byId;
	}

	private <O extends Operation> Map<Method, O> mapOperations(
			ExposableEndpoint<O> endpoint) {
		Map<Method, O> byMethod = new HashMap<>();
		endpoint.getOperations().forEach((operation) -> {
			AbstractDiscoveredOperation discoveredOperation = (AbstractDiscoveredOperation) operation;
			Method method = discoveredOperation.getOperationMethod().getMethod();
			O existing = byMethod.put(method, operation);
			if (existing != null) {
				throw new AssertionError(String.format(
						"Found endpoint with duplicate operation method '%s'", method));
			}
		});
		return byMethod;
	}

	private void load(Class<?> configuration,
			Consumer<AnnotationConfigApplicationContext> consumer) {
		load(null, configuration, consumer);
	}

	private void loadWithParent(ApplicationContext parent, Class<?> configuration,
			Consumer<AnnotationConfigApplicationContext> consumer) {
		load(parent, configuration, consumer);
	}

	private void load(ApplicationContext parent, Class<?> configuration,
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

	@Import({ TestEndpoint.class, SpecializedTestEndpoint.class,
			SpecializedExtension.class })
	static class SpecializedEndpointsConfiguration {

	}

	@Import({ TestEndpoint.class, SubSpecializedTestEndpoint.class,
			SpecializedExtension.class })
	static class SubSpecializedEndpointsConfiguration {

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

	static class SpecializedEndpointFilter extends DiscovererEndpointFilter {

		SpecializedEndpointFilter() {
			super(SpecializedEndpointDiscoverer.class);
		}

	}

	@SpecializedEndpoint(id = "specialized")
	static class SpecializedTestEndpoint {

		@ReadOperation
		public Object getAll() {
			return null;
		}

	}

	static class SubSpecializedTestEndpoint extends SpecializedTestEndpoint {

		@ReadOperation
		public Object getSpecialOne(@Selector String id) {
			return null;
		}

	}

	static class TestEndpointDiscoverer
			extends EndpointDiscoverer<TestExposableEndpoint, TestOperation> {

		TestEndpointDiscoverer(ApplicationContext applicationContext) {
			this(applicationContext, (id) -> null);
		}

		TestEndpointDiscoverer(ApplicationContext applicationContext,
				Function<String, Long> timeToLive) {
			this(applicationContext, timeToLive, Collections.emptyList());
		}

		TestEndpointDiscoverer(ApplicationContext applicationContext,
				Function<String, Long> timeToLive,
				Collection<EndpointFilter<TestExposableEndpoint>> filters) {
			this(applicationContext, new ConversionServiceParameterValueMapper(),
					Collections.singleton(new CachingOperationInvokerAdvisor(timeToLive)),
					filters);
		}

		TestEndpointDiscoverer(ApplicationContext applicationContext,
				ParameterValueMapper parameterValueMapper,
				Collection<OperationInvokerAdvisor> invokerAdvisors,
				Collection<EndpointFilter<TestExposableEndpoint>> filters) {
			super(applicationContext, parameterValueMapper, invokerAdvisors, filters);
		}

		@Override
		protected TestExposableEndpoint createEndpoint(Object endpointBean, String id,
				boolean enabledByDefault, Collection<TestOperation> operations) {
			return new TestExposableEndpoint(this, endpointBean, id, enabledByDefault,
					operations);
		}

		@Override
		protected TestOperation createOperation(String endpointId,
				DiscoveredOperationMethod operationMethod, OperationInvoker invoker) {
			return new TestOperation(operationMethod, invoker);
		}

		@Override
		protected OperationKey createOperationKey(TestOperation operation) {
			return new OperationKey(operation.getOperationMethod(),
					() -> "TestOperation " + operation.getOperationMethod());
		}

	}

	static class SpecializedEndpointDiscoverer extends
			EndpointDiscoverer<SpecializedExposableEndpoint, SpecializedOperation> {

		SpecializedEndpointDiscoverer(ApplicationContext applicationContext) {
			this(applicationContext, Collections.emptyList());
		}

		SpecializedEndpointDiscoverer(ApplicationContext applicationContext,
				Collection<EndpointFilter<SpecializedExposableEndpoint>> filters) {
			super(applicationContext, new ConversionServiceParameterValueMapper(),
					Collections.emptyList(), filters);
		}

		@Override
		protected SpecializedExposableEndpoint createEndpoint(Object endpointBean,
				String id, boolean enabledByDefault,
				Collection<SpecializedOperation> operations) {
			return new SpecializedExposableEndpoint(this, endpointBean, id,
					enabledByDefault, operations);
		}

		@Override
		protected SpecializedOperation createOperation(String endpointId,
				DiscoveredOperationMethod operationMethod, OperationInvoker invoker) {
			return new SpecializedOperation(operationMethod, invoker);
		}

		@Override
		protected OperationKey createOperationKey(SpecializedOperation operation) {
			return new OperationKey(operation.getOperationMethod(),
					() -> "TestOperation " + operation.getOperationMethod());
		}

	}

	static class TestExposableEndpoint extends AbstractDiscoveredEndpoint<TestOperation> {

		TestExposableEndpoint(EndpointDiscoverer<?, ?> discoverer, Object endpointBean,
				String id, boolean enabledByDefault,
				Collection<? extends TestOperation> operations) {
			super(discoverer, endpointBean, id, enabledByDefault, operations);
		}

	}

	static class SpecializedExposableEndpoint
			extends AbstractDiscoveredEndpoint<SpecializedOperation> {

		SpecializedExposableEndpoint(EndpointDiscoverer<?, ?> discoverer,
				Object endpointBean, String id, boolean enabledByDefault,
				Collection<? extends SpecializedOperation> operations) {
			super(discoverer, endpointBean, id, enabledByDefault, operations);
		}

	}

	static class TestOperation extends AbstractDiscoveredOperation {

		private final OperationInvoker invoker;

		TestOperation(DiscoveredOperationMethod operationMethod,
				OperationInvoker invoker) {
			super(operationMethod, invoker);
			this.invoker = invoker;
		}

		public OperationInvoker getInvoker() {
			return this.invoker;
		}

	}

	static class SpecializedOperation extends TestOperation {

		SpecializedOperation(DiscoveredOperationMethod operationMethod,
				OperationInvoker invoker) {
			super(operationMethod, invoker);
		}

	}

}
