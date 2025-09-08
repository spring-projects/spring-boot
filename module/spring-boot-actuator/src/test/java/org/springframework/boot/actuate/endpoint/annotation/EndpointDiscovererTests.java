/*
 * Copyright 2012-present the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.endpoint.Access;
import org.springframework.boot.actuate.endpoint.EndpointFilter;
import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.ExposableEndpoint;
import org.springframework.boot.actuate.endpoint.Operation;
import org.springframework.boot.actuate.endpoint.OperationFilter;
import org.springframework.boot.actuate.endpoint.OperationType;
import org.springframework.boot.actuate.endpoint.invoke.OperationInvoker;
import org.springframework.boot.actuate.endpoint.invoke.OperationInvokerAdvisor;
import org.springframework.boot.actuate.endpoint.invoke.ParameterValueMapper;
import org.springframework.boot.actuate.endpoint.invoke.convert.ConversionServiceParameterValueMapper;
import org.springframework.boot.actuate.endpoint.invoker.cache.CachingOperationInvoker;
import org.springframework.boot.actuate.endpoint.invoker.cache.CachingOperationInvokerAdvisor;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.FixedValue;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link EndpointDiscoverer}.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
class EndpointDiscovererTests {

	@Test
	void createWhenApplicationContextIsNullShouldThrowException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new TestEndpointDiscoverer(null, mock(ParameterValueMapper.class),
					Collections.emptyList(), Collections.emptyList()))
			.withMessageContaining("'applicationContext' must not be null");
	}

	@Test
	void createWhenParameterValueMapperIsNullShouldThrowException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new TestEndpointDiscoverer(mock(ApplicationContext.class), null, Collections.emptyList(),
					Collections.emptyList()))
			.withMessageContaining("'parameterValueMapper' must not be null");
	}

	@Test
	void createWhenInvokerAdvisorsIsNullShouldThrowException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new TestEndpointDiscoverer(mock(ApplicationContext.class),
					mock(ParameterValueMapper.class), null, Collections.emptyList()))
			.withMessageContaining("'invokerAdvisors' must not be null");
	}

	@Test
	void createWhenFiltersIsNullShouldThrowException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new TestEndpointDiscoverer(mock(ApplicationContext.class),
					mock(ParameterValueMapper.class), Collections.emptyList(), null))
			.withMessageContaining("'endpointFilters' must not be null");
	}

	@Test
	void getEndpointsWhenNoEndpointBeansShouldReturnEmptyCollection() {
		load(EmptyConfiguration.class, (context) -> {
			TestEndpointDiscoverer discoverer = new TestEndpointDiscoverer(context);
			Collection<TestExposableEndpoint> endpoints = discoverer.getEndpoints();
			assertThat(endpoints).isEmpty();
		});
	}

	@Test
	void getEndpointsWhenHasEndpointShouldReturnEndpoint() {
		load(TestEndpointConfiguration.class, this::hasTestEndpoint);
	}

	@Test
	void getEndpointsWhenHasEndpointInParentContextShouldReturnEndpoint() {
		AnnotationConfigApplicationContext parent = new AnnotationConfigApplicationContext(
				TestEndpointConfiguration.class);
		loadWithParent(parent, EmptyConfiguration.class, this::hasTestEndpoint);
	}

	@Test
	void getEndpointsWhenHasSubclassedEndpointShouldReturnEndpoint() {
		load(TestEndpointSubclassConfiguration.class, (context) -> {
			TestEndpointDiscoverer discoverer = new TestEndpointDiscoverer(context);
			Map<EndpointId, TestExposableEndpoint> endpoints = mapEndpoints(discoverer.getEndpoints());
			assertThat(endpoints).containsOnlyKeys(EndpointId.of("test"));
			Map<Method, TestOperation> operations = mapOperations(endpoints.get(EndpointId.of("test")));
			assertThat(operations).hasSize(5);
			assertThat(operations).containsKeys(testEndpointMethods());
			assertThat(operations).containsKeys(ReflectionUtils.findMethod(TestEndpointSubclass.class,
					"updateWithMoreArguments", String.class, String.class, String.class));
		});
	}

	@Test
	void getEndpointsWhenTwoEndpointsHaveTheSameIdShouldThrowException() {
		load(ClashingEndpointConfiguration.class,
				(context) -> assertThatIllegalStateException()
					.isThrownBy(new TestEndpointDiscoverer(context)::getEndpoints)
					.withMessageContaining("Found two endpoints with the id 'test': "));
	}

	@Test
	void getEndpointsWhenEndpointsArePrefixedWithScopedTargetShouldRegisterOnlyOneEndpoint() {
		load(ScopedTargetEndpointConfiguration.class, (context) -> {
			TestEndpoint expectedEndpoint = context.getBean("testEndpoint", TestEndpoint.class);
			Collection<TestExposableEndpoint> endpoints = new TestEndpointDiscoverer(context).getEndpoints();
			assertThat(endpoints).flatExtracting(TestExposableEndpoint::getEndpointBean).containsOnly(expectedEndpoint);
		});
	}

	@Test
	void getEndpointsWhenTtlSetToZeroShouldNotCacheInvokeCalls() {
		load(TestEndpointConfiguration.class, (context) -> {
			TestEndpointDiscoverer discoverer = new TestEndpointDiscoverer(context, (endpointId) -> 0L);
			Map<EndpointId, TestExposableEndpoint> endpoints = mapEndpoints(discoverer.getEndpoints());
			assertThat(endpoints).containsOnlyKeys(EndpointId.of("test"));
			Map<Method, TestOperation> operations = mapOperations(endpoints.get(EndpointId.of("test")));
			operations.values()
				.forEach((operation) -> assertThat(operation.getInvoker())
					.isNotInstanceOf(CachingOperationInvoker.class));
		});
	}

	@Test
	void getEndpointsWhenTtlSetByIdAndIdDoesNotMatchShouldNotCacheInvokeCalls() {
		load(TestEndpointConfiguration.class, (context) -> {
			TestEndpointDiscoverer discoverer = new TestEndpointDiscoverer(context,
					(endpointId) -> (endpointId.equals(EndpointId.of("foo")) ? 500L : 0L));
			Map<EndpointId, TestExposableEndpoint> endpoints = mapEndpoints(discoverer.getEndpoints());
			assertThat(endpoints).containsOnlyKeys(EndpointId.of("test"));
			Map<Method, TestOperation> operations = mapOperations(endpoints.get(EndpointId.of("test")));
			operations.values()
				.forEach((operation) -> assertThat(operation.getInvoker())
					.isNotInstanceOf(CachingOperationInvoker.class));
		});
	}

	@Test
	void getEndpointsWhenTtlSetByIdAndIdMatchesShouldCacheInvokeCalls() {
		load(TestEndpointConfiguration.class, (context) -> {
			TestEndpointDiscoverer discoverer = new TestEndpointDiscoverer(context,
					(endpointId) -> (endpointId.equals(EndpointId.of("test")) ? 500L : 0L));
			Map<EndpointId, TestExposableEndpoint> endpoints = mapEndpoints(discoverer.getEndpoints());
			assertThat(endpoints).containsOnlyKeys(EndpointId.of("test"));
			Map<Method, TestOperation> operations = mapOperations(endpoints.get(EndpointId.of("test")));
			TestOperation getAll = operations.get(findTestEndpointMethod("getAll"));
			TestOperation getOne = operations.get(findTestEndpointMethod("getOne", String.class));
			TestOperation update = operations
				.get(ReflectionUtils.findMethod(TestEndpoint.class, "update", String.class, String.class));
			assertThat(((CachingOperationInvoker) getAll.getInvoker()).getTimeToLive()).isEqualTo(500);
			assertThat(getOne.getInvoker()).isNotInstanceOf(CachingOperationInvoker.class);
			assertThat(update.getInvoker()).isNotInstanceOf(CachingOperationInvoker.class);
		});
	}

	@Test
	void getEndpointsWhenHasSpecializedFiltersInNonSpecializedDiscovererShouldFilterEndpoints() {
		load(SpecializedEndpointsConfiguration.class, (context) -> {
			TestEndpointDiscoverer discoverer = new TestEndpointDiscoverer(context);
			Map<EndpointId, TestExposableEndpoint> endpoints = mapEndpoints(discoverer.getEndpoints());
			assertThat(endpoints).containsOnlyKeys(EndpointId.of("test"));
		});
	}

	@Test
	void getEndpointsWhenHasSpecializedFiltersInSpecializedDiscovererShouldNotFilterEndpoints() {
		load(SpecializedEndpointsConfiguration.class, (context) -> {
			SpecializedEndpointDiscoverer discoverer = new SpecializedEndpointDiscoverer(context);
			Map<EndpointId, SpecializedExposableEndpoint> endpoints = mapEndpoints(discoverer.getEndpoints());
			assertThat(endpoints).containsOnlyKeys(EndpointId.of("test"), EndpointId.of("specialized"),
					EndpointId.of("specialized-superclass"));
		});
	}

	@Test
	void getEndpointsShouldApplyExtensions() {
		load(SpecializedEndpointsConfiguration.class, (context) -> {
			SpecializedEndpointDiscoverer discoverer = new SpecializedEndpointDiscoverer(context);
			Map<EndpointId, SpecializedExposableEndpoint> endpoints = mapEndpoints(discoverer.getEndpoints());
			Map<Method, SpecializedOperation> operations = mapOperations(endpoints.get(EndpointId.of("specialized")));
			assertThat(operations).containsKeys(ReflectionUtils.findMethod(SpecializedExtension.class, "getSpecial"));

		});
	}

	@Test
	void getEndpointShouldFindParentExtension() {
		load(SubSpecializedEndpointsConfiguration.class, (context) -> {
			SpecializedEndpointDiscoverer discoverer = new SpecializedEndpointDiscoverer(context);
			Map<EndpointId, SpecializedExposableEndpoint> endpoints = mapEndpoints(discoverer.getEndpoints());
			Map<Method, SpecializedOperation> operations = mapOperations(endpoints.get(EndpointId.of("specialized")));
			assertThat(operations).containsKeys(ReflectionUtils.findMethod(SpecializedTestEndpoint.class, "getAll"));
			assertThat(operations).containsKeys(
					ReflectionUtils.findMethod(SubSpecializedTestEndpoint.class, "getSpecialOne", String.class));
			assertThat(operations).containsKeys(ReflectionUtils.findMethod(SpecializedExtension.class, "getSpecial"));
			assertThat(operations).hasSize(3);
		});
	}

	@Test
	void getEndpointsWhenHasProxiedEndpointShouldReturnEndpoint() {
		load(ProxiedSpecializedEndpointsConfiguration.class, (context) -> {
			SpecializedEndpointDiscoverer discoverer = new SpecializedEndpointDiscoverer(context);
			Map<EndpointId, SpecializedExposableEndpoint> endpoints = mapEndpoints(discoverer.getEndpoints());
			assertThat(endpoints).containsOnlyKeys(EndpointId.of("test"), EndpointId.of("specialized"));
		});
	}

	@Test
	void getEndpointsShouldApplyEndpointFilters() {
		load(SpecializedEndpointsConfiguration.class, (context) -> {
			EndpointFilter<SpecializedExposableEndpoint> filter = (endpoint) -> {
				EndpointId id = endpoint.getEndpointId();
				return !id.equals(EndpointId.of("specialized")) && !id.equals(EndpointId.of("specialized-superclass"));
			};
			SpecializedEndpointDiscoverer discoverer = new SpecializedEndpointDiscoverer(context,
					Collections.singleton(filter), Collections.emptyList());
			Map<EndpointId, SpecializedExposableEndpoint> endpoints = mapEndpoints(discoverer.getEndpoints());
			assertThat(endpoints).containsOnlyKeys(EndpointId.of("test"));
		});
	}

	@Test
	void getEndpointsShouldApplyOperationFilters() {
		load(SpecializedEndpointsConfiguration.class, (context) -> {
			OperationFilter<SpecializedOperation> operationFilter = (operation, endpointId,
					defaultAccess) -> operation.getType() == OperationType.READ;
			SpecializedEndpointDiscoverer discoverer = new SpecializedEndpointDiscoverer(context,
					Collections.emptyList(), List.of(operationFilter));
			Map<EndpointId, SpecializedExposableEndpoint> endpoints = mapEndpoints(discoverer.getEndpoints());
			assertThat(endpoints.values())
				.allSatisfy((endpoint) -> assertThat(endpoint.getOperations()).extracting(SpecializedOperation::getType)
					.containsOnly(OperationType.READ));
		});
	}

	private void hasTestEndpoint(AnnotationConfigApplicationContext context) {
		TestEndpointDiscoverer discoverer = new TestEndpointDiscoverer(context);
		Map<EndpointId, TestExposableEndpoint> endpoints = mapEndpoints(discoverer.getEndpoints());
		assertThat(endpoints).containsOnlyKeys(EndpointId.of("test"));
		Map<Method, TestOperation> operations = mapOperations(endpoints.get(EndpointId.of("test")));
		assertThat(operations).containsOnlyKeys(testEndpointMethods());
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

	private <E extends ExposableEndpoint<?>> Map<EndpointId, E> mapEndpoints(Collection<E> endpoints) {
		Map<EndpointId, E> byId = new LinkedHashMap<>();
		endpoints.forEach((endpoint) -> {
			E existing = byId.put(endpoint.getEndpointId(), endpoint);
			if (existing != null) {
				throw new AssertionError(
						String.format("Found endpoints with duplicate id '%s'", endpoint.getEndpointId()));
			}
		});
		return byId;
	}

	private <O extends Operation> Map<Method, O> mapOperations(ExposableEndpoint<O> endpoint) {
		Map<Method, O> byMethod = new HashMap<>();
		endpoint.getOperations().forEach((operation) -> {
			AbstractDiscoveredOperation discoveredOperation = (AbstractDiscoveredOperation) operation;
			Method method = discoveredOperation.getOperationMethod().getMethod();
			O existing = byMethod.put(method, operation);
			if (existing != null) {
				throw new AssertionError(String.format("Found endpoint with duplicate operation method '%s'", method));
			}
		});
		return byMethod;
	}

	private void load(Class<?> configuration, Consumer<AnnotationConfigApplicationContext> consumer) {
		load(null, configuration, consumer);
	}

	private void loadWithParent(ApplicationContext parent, Class<?> configuration,
			Consumer<AnnotationConfigApplicationContext> consumer) {
		load(parent, configuration, consumer);
	}

	private void load(ApplicationContext parent, Class<?> configuration,
			Consumer<AnnotationConfigApplicationContext> consumer) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		try (context) {
			if (parent != null) {
				context.setParent(parent);
			}
			context.register(configuration);
			context.refresh();
			consumer.accept(context);
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class EmptyConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	static class ProxiedSpecializedTestEndpointConfiguration {

		@Bean
		SpecializedExtension specializedExtension() {
			Enhancer enhancer = new Enhancer();
			enhancer.setSuperclass(SpecializedExtension.class);
			enhancer.setCallback((FixedValue) () -> null);
			return (SpecializedExtension) enhancer.create();
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
	static class TestEndpointSubclassConfiguration {

		@Bean
		TestEndpointSubclass testEndpointSubclass() {
			return new TestEndpointSubclass();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ClashingEndpointConfiguration {

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
	static class ScopedTargetEndpointConfiguration {

		@Bean
		TestEndpoint testEndpoint() {
			return new TestEndpoint();
		}

		@Bean(name = "scopedTarget.testEndpoint")
		TestEndpoint scopedTargetTestEndpoint() {
			return new TestEndpoint();
		}

	}

	@Import({ TestEndpoint.class, SpecializedTestEndpoint.class, SpecializedSuperclassTestEndpoint.class,
			SpecializedExtension.class })
	static class SpecializedEndpointsConfiguration {

	}

	@Import({ TestEndpoint.class, SubSpecializedTestEndpoint.class, SpecializedExtension.class })
	static class SubSpecializedEndpointsConfiguration {

	}

	@Import({ TestEndpoint.class, SpecializedTestEndpoint.class, ProxiedSpecializedTestEndpointConfiguration.class })
	static class ProxiedSpecializedEndpointsConfiguration {

	}

	@Endpoint(id = "test")
	static class TestEndpoint {

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

		@DeleteOperation
		void deleteOne(@Selector String id) {

		}

		void someOtherMethod() {

		}

	}

	static class TestEndpointSubclass extends TestEndpoint {

		@WriteOperation
		void updateWithMoreArguments(String foo, String bar, String baz) {

		}

	}

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Endpoint
	@FilteredEndpoint(SpecializedEndpointFilter.class)
	@interface SpecializedEndpoint {

		@AliasFor(annotation = Endpoint.class)
		String id();

	}

	@EndpointExtension(endpoint = SpecializedTestEndpoint.class, filter = SpecializedEndpointFilter.class)
	static class SpecializedExtension {

		@ReadOperation
		Object getSpecial() {
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
		Object getAll() {
			return null;
		}

	}

	@SpecializedEndpoint(id = "specialized-superclass")
	static class AbstractFilteredEndpoint {

	}

	static class SpecializedSuperclassTestEndpoint extends AbstractFilteredEndpoint {

		@ReadOperation
		Object getAll() {
			return null;
		}

	}

	static class SubSpecializedTestEndpoint extends SpecializedTestEndpoint {

		@ReadOperation
		Object getSpecialOne(@Selector String id) {
			return null;
		}

	}

	static class TestEndpointDiscoverer extends EndpointDiscoverer<TestExposableEndpoint, TestOperation> {

		TestEndpointDiscoverer(ApplicationContext applicationContext) {
			this(applicationContext, (id) -> null);
		}

		TestEndpointDiscoverer(ApplicationContext applicationContext, Function<EndpointId, Long> timeToLive) {
			this(applicationContext, timeToLive, Collections.emptyList());
		}

		TestEndpointDiscoverer(ApplicationContext applicationContext, Function<EndpointId, Long> timeToLive,
				Collection<EndpointFilter<TestExposableEndpoint>> filters) {
			this(applicationContext, new ConversionServiceParameterValueMapper(),
					Collections.singleton(new CachingOperationInvokerAdvisor(timeToLive)), filters);
		}

		TestEndpointDiscoverer(ApplicationContext applicationContext, ParameterValueMapper parameterValueMapper,
				Collection<OperationInvokerAdvisor> invokerAdvisors,
				Collection<EndpointFilter<TestExposableEndpoint>> filters) {
			super(applicationContext, parameterValueMapper, invokerAdvisors, filters, Collections.emptyList());
		}

		@Override
		protected TestExposableEndpoint createEndpoint(Object endpointBean, EndpointId id, Access defaultAccess,
				Collection<TestOperation> operations) {
			return new TestExposableEndpoint(this, endpointBean, id, defaultAccess, operations);
		}

		@Override
		protected TestOperation createOperation(EndpointId endpointId, DiscoveredOperationMethod operationMethod,
				OperationInvoker invoker) {
			return new TestOperation(operationMethod, invoker);
		}

		@Override
		protected OperationKey createOperationKey(TestOperation operation) {
			return new OperationKey(operation.getOperationMethod(),
					() -> "TestOperation " + operation.getOperationMethod());
		}

	}

	static class SpecializedEndpointDiscoverer
			extends EndpointDiscoverer<SpecializedExposableEndpoint, SpecializedOperation> {

		SpecializedEndpointDiscoverer(ApplicationContext applicationContext) {
			this(applicationContext, Collections.emptyList(), Collections.emptyList());
		}

		SpecializedEndpointDiscoverer(ApplicationContext applicationContext,
				Collection<EndpointFilter<SpecializedExposableEndpoint>> filters,
				Collection<OperationFilter<SpecializedOperation>> operationFilters) {
			super(applicationContext, new ConversionServiceParameterValueMapper(), Collections.emptyList(), filters,
					operationFilters);
		}

		@Override
		protected SpecializedExposableEndpoint createEndpoint(Object endpointBean, EndpointId id, Access defaultAccess,
				Collection<SpecializedOperation> operations) {
			return new SpecializedExposableEndpoint(this, endpointBean, id, defaultAccess, operations);
		}

		@Override
		protected SpecializedOperation createOperation(EndpointId endpointId, DiscoveredOperationMethod operationMethod,
				OperationInvoker invoker) {
			return new SpecializedOperation(operationMethod, invoker);
		}

		@Override
		protected OperationKey createOperationKey(SpecializedOperation operation) {
			return new OperationKey(operation.getOperationMethod(),
					() -> "TestOperation " + operation.getOperationMethod());
		}

	}

	static class TestExposableEndpoint extends AbstractDiscoveredEndpoint<TestOperation> {

		TestExposableEndpoint(EndpointDiscoverer<?, ?> discoverer, Object endpointBean, EndpointId id,
				Access defaultAccess, Collection<? extends TestOperation> operations) {
			super(discoverer, endpointBean, id, defaultAccess, operations);
		}

	}

	static class SpecializedExposableEndpoint extends AbstractDiscoveredEndpoint<SpecializedOperation> {

		SpecializedExposableEndpoint(EndpointDiscoverer<?, ?> discoverer, Object endpointBean, EndpointId id,
				Access defaultAccess, Collection<? extends SpecializedOperation> operations) {
			super(discoverer, endpointBean, id, defaultAccess, operations);
		}

	}

	static class TestOperation extends AbstractDiscoveredOperation {

		private final OperationInvoker invoker;

		TestOperation(DiscoveredOperationMethod operationMethod, OperationInvoker invoker) {
			super(operationMethod, invoker);
			this.invoker = invoker;
		}

		OperationInvoker getInvoker() {
			return this.invoker;
		}

	}

	static class SpecializedOperation extends TestOperation {

		SpecializedOperation(DiscoveredOperationMethod operationMethod, OperationInvoker invoker) {
			super(operationMethod, invoker);
		}

	}

}
