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

package org.springframework.boot.actuate.endpoint.jmx;

import java.util.ArrayList;
import java.util.List;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.jmx.JmxException;
import org.springframework.jmx.export.MBeanExportException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

/**
 * Tests for {@link JmxEndpointExporter}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
@ExtendWith(MockitoExtension.class)
class JmxEndpointExporterTests {

	private final JmxOperationResponseMapper responseMapper = new TestJmxOperationResponseMapper();

	private final List<ExposableJmxEndpoint> endpoints = new ArrayList<>();

	@Mock
	@SuppressWarnings("NullAway.Init")
	private MBeanServer mBeanServer;

	@Spy
	@SuppressWarnings("NullAway.Init")
	private EndpointObjectNameFactory objectNameFactory = new TestEndpointObjectNameFactory();

	private JmxEndpointExporter exporter;

	@BeforeEach
	void setup() {
		this.exporter = new JmxEndpointExporter(this.mBeanServer, this.objectNameFactory, this.responseMapper,
				this.endpoints);
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void createWhenMBeanServerIsNullShouldThrowException() {
		assertThatIllegalArgumentException()
			.isThrownBy(
					() -> new JmxEndpointExporter(null, this.objectNameFactory, this.responseMapper, this.endpoints))
			.withMessageContaining("'mBeanServer' must not be null");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void createWhenObjectNameFactoryIsNullShouldThrowException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new JmxEndpointExporter(this.mBeanServer, null, this.responseMapper, this.endpoints))
			.withMessageContaining("'objectNameFactory' must not be null");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void createWhenResponseMapperIsNullShouldThrowException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new JmxEndpointExporter(this.mBeanServer, this.objectNameFactory, null, this.endpoints))
			.withMessageContaining("'responseMapper' must not be null");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void createWhenEndpointsIsNullShouldThrowException() {
		assertThatIllegalArgumentException()
			.isThrownBy(
					() -> new JmxEndpointExporter(this.mBeanServer, this.objectNameFactory, this.responseMapper, null))
			.withMessageContaining("'endpoints' must not be null");
	}

	@Test
	void afterPropertiesSetShouldRegisterMBeans() throws Exception {
		this.endpoints.add(new TestExposableJmxEndpoint(new TestJmxOperation()));
		this.exporter.afterPropertiesSet();
		then(this.mBeanServer).should()
			.registerMBean(assertArg((object) -> assertThat(object).isInstanceOf(EndpointMBean.class)),
					assertArg((objectName) -> assertThat(objectName.getKeyProperty("name")).isEqualTo("test")));
	}

	@Test
	void registerShouldUseObjectNameFactory() throws Exception {
		this.endpoints.add(new TestExposableJmxEndpoint(new TestJmxOperation()));
		this.exporter.afterPropertiesSet();
		then(this.objectNameFactory).should().getObjectName(any(ExposableJmxEndpoint.class));
	}

	@Test
	void registerWhenObjectNameIsMalformedShouldThrowException() throws Exception {
		TestExposableJmxEndpoint endpoint = new TestExposableJmxEndpoint(new TestJmxOperation());
		given(this.objectNameFactory.getObjectName(endpoint)).willThrow(MalformedObjectNameException.class);
		this.endpoints.add(endpoint);
		assertThatIllegalStateException().isThrownBy(this.exporter::afterPropertiesSet)
			.withMessageContaining("Invalid ObjectName for endpoint 'test'");
	}

	@Test
	void registerWhenRegistrationFailsShouldThrowException() throws Exception {
		given(this.mBeanServer.registerMBean(any(), any(ObjectName.class)))
			.willThrow(new MBeanRegistrationException(new RuntimeException()));
		this.endpoints.add(new TestExposableJmxEndpoint(new TestJmxOperation()));
		assertThatExceptionOfType(MBeanExportException.class).isThrownBy(this.exporter::afterPropertiesSet)
			.withMessageContaining("Failed to register MBean for endpoint 'test");
	}

	@Test
	void registerWhenEndpointHasNoOperationsShouldNotCreateMBean() {
		this.endpoints.add(new TestExposableJmxEndpoint());
		this.exporter.afterPropertiesSet();
		then(this.mBeanServer).shouldHaveNoInteractions();
	}

	@Test
	void destroyShouldUnregisterMBeans() throws Exception {
		this.endpoints.add(new TestExposableJmxEndpoint(new TestJmxOperation()));
		this.exporter.afterPropertiesSet();
		this.exporter.destroy();
		then(this.mBeanServer).should()
			.unregisterMBean(
					assertArg((objectName) -> assertThat(objectName.getKeyProperty("name")).isEqualTo("test")));
	}

	@Test
	void unregisterWhenInstanceNotFoundShouldContinue() throws Exception {
		this.endpoints.add(new TestExposableJmxEndpoint(new TestJmxOperation()));
		this.exporter.afterPropertiesSet();
		willThrow(InstanceNotFoundException.class).given(this.mBeanServer).unregisterMBean(any(ObjectName.class));
		this.exporter.destroy();
	}

	@Test
	void unregisterWhenUnregisterThrowsExceptionShouldThrowException() throws Exception {
		this.endpoints.add(new TestExposableJmxEndpoint(new TestJmxOperation()));
		this.exporter.afterPropertiesSet();
		willThrow(new MBeanRegistrationException(new RuntimeException())).given(this.mBeanServer)
			.unregisterMBean(any(ObjectName.class));
		assertThatExceptionOfType(JmxException.class).isThrownBy(() -> this.exporter.destroy())
			.withMessageContaining("Failed to unregister MBean with ObjectName 'boot");
	}

	/**
	 * Test {@link EndpointObjectNameFactory}.
	 */
	static class TestEndpointObjectNameFactory implements EndpointObjectNameFactory {

		@Override
		public ObjectName getObjectName(ExposableJmxEndpoint endpoint) throws MalformedObjectNameException {
			return new ObjectName("boot:type=Endpoint,name=" + endpoint.getEndpointId());
		}

	}

}
