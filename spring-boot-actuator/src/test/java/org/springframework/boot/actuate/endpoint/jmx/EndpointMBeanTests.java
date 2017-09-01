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

package org.springframework.boot.actuate.endpoint.jmx;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.endpoint.annotation.DeleteOperation;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.boot.actuate.endpoint.cache.CachingConfiguration;
import org.springframework.boot.actuate.endpoint.convert.ConversionServiceOperationParameterMapper;
import org.springframework.boot.actuate.endpoint.jmx.annotation.JmxAnnotationEndpointDiscoverer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.convert.support.DefaultConversionService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EndpointMBean}.
 *
 * @author Stephane Nicoll
 */
public class EndpointMBeanTests {

	private final JmxEndpointMBeanFactory jmxEndpointMBeanFactory = new JmxEndpointMBeanFactory(
			new TestJmxOperationResponseMapper());

	private MBeanServer server;

	private EndpointMBeanRegistrar endpointMBeanRegistrar;

	private EndpointObjectNameFactory objectNameFactory = (endpoint) -> new ObjectName(
			String.format("org.springframework.boot.test:type=Endpoint,name=%s",
					UUID.randomUUID().toString()));

	@Before
	public void createMBeanServer() {
		this.server = MBeanServerFactory.createMBeanServer();
		this.endpointMBeanRegistrar = new EndpointMBeanRegistrar(this.server,
				this.objectNameFactory);
	}

	@After
	public void disposeMBeanServer() {
		if (this.server != null) {
			MBeanServerFactory.releaseMBeanServer(this.server);
		}
	}

	@Test
	public void invokeSimpleEndpoint() {
		load(FooEndpoint.class, (discoverer) -> {
			ObjectName objectName = registerEndpoint(discoverer, "foo");
			try {
				// getAll
				Object allResponse = this.server.invoke(objectName, "getAll",
						new Object[0], new String[0]);
				assertThat(allResponse).isEqualTo("[ONE, TWO]");

				// getOne
				Object oneResponse = this.server.invoke(objectName, "getOne",
						new Object[] { "one" }, new String[] { String.class.getName() });
				assertThat(oneResponse).isEqualTo("ONE");

				// update
				Object updateResponse = this.server.invoke(objectName, "update",
						new Object[] { "one", "1" },
						new String[] { String.class.getName(), String.class.getName() });
				assertThat(updateResponse).isNull();

				// getOne validation after update
				Object updatedOneResponse = this.server.invoke(objectName, "getOne",
						new Object[] { "one" }, new String[] { String.class.getName() });
				assertThat(updatedOneResponse).isEqualTo("1");

				// deleteOne
				Object deleteResponse = this.server.invoke(objectName, "deleteOne",
						new Object[] { "one" }, new String[] { String.class.getName() });
				assertThat(deleteResponse).isNull();

				// getOne validation after delete
				updatedOneResponse = this.server.invoke(objectName, "getOne",
						new Object[] { "one" }, new String[] { String.class.getName() });
				assertThat(updatedOneResponse).isNull();
			}
			catch (Exception ex) {
				throw new AssertionError("Failed to invoke method on FooEndpoint", ex);
			}
		});
	}

	@Test
	public void jmxTypesAreProperlyMapped() {
		load(FooEndpoint.class, (discoverer) -> {
			ObjectName objectName = registerEndpoint(discoverer, "foo");
			try {
				MBeanInfo mBeanInfo = this.server.getMBeanInfo(objectName);
				Map<String, MBeanOperationInfo> operations = mapOperations(mBeanInfo);
				assertThat(operations).containsOnlyKeys("getAll", "getOne", "update",
						"deleteOne");
				assertOperation(operations.get("getAll"), String.class,
						MBeanOperationInfo.INFO, new Class<?>[0]);
				assertOperation(operations.get("getOne"), String.class,
						MBeanOperationInfo.INFO, new Class<?>[] { String.class });
				assertOperation(operations.get("update"), Void.TYPE,
						MBeanOperationInfo.ACTION,
						new Class<?>[] { String.class, String.class });
				assertOperation(operations.get("deleteOne"), Void.TYPE,
						MBeanOperationInfo.ACTION, new Class<?>[] { String.class });
			}
			catch (Exception ex) {
				throw new AssertionError("Failed to retrieve MBeanInfo of FooEndpoint",
						ex);
			}
		});
	}

	private void assertOperation(MBeanOperationInfo operation, Class<?> returnType,
			int impact, Class<?>[] types) {
		assertThat(operation.getReturnType()).isEqualTo(returnType.getName());
		assertThat(operation.getImpact()).isEqualTo(impact);
		MBeanParameterInfo[] signature = operation.getSignature();
		assertThat(signature).hasSize(types.length);
		for (int i = 0; i < types.length; i++) {
			assertThat(signature[i].getType()).isEqualTo(types[0].getName());
		}
	}

	@Test
	public void invokeReactiveOperation() {
		load(ReactiveEndpoint.class, (discoverer) -> {
			ObjectName objectName = registerEndpoint(discoverer, "reactive");
			try {
				Object allResponse = this.server.invoke(objectName, "getInfo",
						new Object[0], new String[0]);
				assertThat(allResponse).isInstanceOf(String.class);
				assertThat(allResponse).isEqualTo("HELLO WORLD");
			}
			catch (Exception ex) {
				throw new AssertionError("Failed to invoke getInfo method", ex);
			}
		});

	}

	@Test
	public void invokeUnknownOperation() {
		load(FooEndpoint.class, (discoverer) -> {
			ObjectName objectName = registerEndpoint(discoverer, "foo");
			try {
				this.server.invoke(objectName, "doesNotExist", new Object[0],
						new String[0]);
				throw new AssertionError(
						"Should have failed to invoke unknown operation");
			}
			catch (ReflectionException ex) {
				assertThat(ex.getCause()).isInstanceOf(IllegalArgumentException.class);
				assertThat(ex.getCause().getMessage()).contains("doesNotExist", "foo");
			}
			catch (MBeanException | InstanceNotFoundException ex) {
				throw new IllegalStateException(ex);
			}

		});
	}

	@Test
	public void dynamicMBeanCannotReadAttribute() {
		load(FooEndpoint.class, (discoverer) -> {
			ObjectName objectName = registerEndpoint(discoverer, "foo");
			try {
				this.server.getAttribute(objectName, "foo");
				throw new AssertionError("Should have failed to read attribute foo");
			}
			catch (Exception ex) {
				assertThat(ex).isInstanceOf(AttributeNotFoundException.class);
			}
		});
	}

	@Test
	public void dynamicMBeanCannotWriteAttribute() {
		load(FooEndpoint.class, (discoverer) -> {
			ObjectName objectName = registerEndpoint(discoverer, "foo");
			try {
				this.server.setAttribute(objectName, new Attribute("foo", "bar"));
				throw new AssertionError("Should have failed to write attribute foo");
			}
			catch (Exception ex) {
				assertThat(ex).isInstanceOf(AttributeNotFoundException.class);
			}
		});
	}

	@Test
	public void dynamicMBeanCannotReadAttributes() {
		load(FooEndpoint.class, (discoverer) -> {
			ObjectName objectName = registerEndpoint(discoverer, "foo");
			try {
				AttributeList attributes = this.server.getAttributes(objectName,
						new String[] { "foo", "bar" });
				assertThat(attributes).isNotNull();
				assertThat(attributes).isEmpty();
			}
			catch (Exception ex) {
				throw new AssertionError("Failed to invoke getAttributes", ex);
			}
		});
	}

	@Test
	public void dynamicMBeanCannotWriteAttributes() {
		load(FooEndpoint.class, (discoverer) -> {
			ObjectName objectName = registerEndpoint(discoverer, "foo");
			try {
				AttributeList attributes = new AttributeList();
				attributes.add(new Attribute("foo", 1));
				attributes.add(new Attribute("bar", 42));
				AttributeList attributesSet = this.server.setAttributes(objectName,
						attributes);
				assertThat(attributesSet).isNotNull();
				assertThat(attributesSet).isEmpty();
			}
			catch (Exception ex) {
				throw new AssertionError("Failed to invoke setAttributes", ex);
			}
		});
	}

	private ObjectName registerEndpoint(JmxAnnotationEndpointDiscoverer discoverer,
			String endpointId) {
		Collection<EndpointMBean> mBeans = this.jmxEndpointMBeanFactory
				.createMBeans(discoverer.discoverEndpoints());
		assertThat(mBeans).hasSize(1);
		EndpointMBean endpointMBean = mBeans.iterator().next();
		assertThat(endpointMBean.getEndpointId()).isEqualTo(endpointId);
		return this.endpointMBeanRegistrar.registerEndpointMBean(endpointMBean);
	}

	private Map<String, MBeanOperationInfo> mapOperations(MBeanInfo info) {
		Map<String, MBeanOperationInfo> operations = new HashMap<>();
		for (MBeanOperationInfo mBeanOperationInfo : info.getOperations()) {
			operations.put(mBeanOperationInfo.getName(), mBeanOperationInfo);
		}
		return operations;
	}

	private void load(Class<?> configuration,
			Consumer<JmxAnnotationEndpointDiscoverer> consumer) {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				configuration)) {
			consumer.accept(new JmxAnnotationEndpointDiscoverer(context,
					new ConversionServiceOperationParameterMapper(
							DefaultConversionService.getSharedInstance()),
					(id) -> new CachingConfiguration(0)));
		}
	}

	@Endpoint(id = "foo")
	static class FooEndpoint {

		private final Map<FooName, Foo> all = new LinkedHashMap<>();

		FooEndpoint() {
			this.all.put(FooName.ONE, new Foo("one"));
			this.all.put(FooName.TWO, new Foo("two"));
		}

		@ReadOperation
		public Collection<Foo> getAll() {
			return this.all.values();
		}

		@ReadOperation
		public Foo getOne(FooName name) {
			return this.all.get(name);
		}

		@WriteOperation
		public void update(FooName name, String value) {
			this.all.put(name, new Foo(value));
		}

		@DeleteOperation
		public void deleteOne(FooName name) {
			this.all.remove(name);
		}

	}

	@Endpoint(id = "reactive")
	static class ReactiveEndpoint {

		@ReadOperation
		public Mono<String> getInfo() {
			return Mono.defer(() -> Mono.just("Hello World"));
		}

	}

	enum FooName {

		ONE, TWO, THREE

	}

	static class Foo {

		private final String name;

		Foo(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}

		@Override
		public String toString() {
			return this.name;
		}

	}

	private static class TestJmxOperationResponseMapper
			implements JmxOperationResponseMapper {

		@Override
		public Object mapResponse(Object response) {
			return (response != null ? response.toString().toUpperCase() : null);
		}

		@Override
		public Class<?> mapResponseType(Class<?> responseType) {
			if (responseType == Void.TYPE) {
				return Void.TYPE;
			}
			return String.class;
		}
	}

}
