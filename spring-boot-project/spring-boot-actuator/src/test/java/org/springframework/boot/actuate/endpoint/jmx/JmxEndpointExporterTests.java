/*
 * Copyright 2012-2018 the original author or authors.
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

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.jmx.JmxException;
import org.springframework.jmx.export.MBeanExportException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link JmxEndpointExporter}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
public class JmxEndpointExporterTests {

	@Mock
	private MBeanServer mBeanServer;

	private EndpointObjectNameFactory objectNameFactory = spy(
			new TestEndpointObjectNameFactory());

	private JmxOperationResponseMapper responseMapper = new TestJmxOperationResponseMapper();

	private List<ExposableJmxEndpoint> endpoints = new ArrayList<>();

	@Captor
	private ArgumentCaptor<Object> objectCaptor;

	@Captor
	private ArgumentCaptor<ObjectName> objectNameCaptor;

	private JmxEndpointExporter exporter;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		this.exporter = new JmxEndpointExporter(this.mBeanServer, this.objectNameFactory,
				this.responseMapper, this.endpoints);
	}

	@Test
	public void createWhenMBeanServerIsNullShouldThrowException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new JmxEndpointExporter(null, this.objectNameFactory,
						this.responseMapper, this.endpoints))
				.withMessageContaining("MBeanServer must not be null");
	}

	@Test
	public void createWhenObjectNameFactoryIsNullShouldThrowException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new JmxEndpointExporter(this.mBeanServer, null,
						this.responseMapper, this.endpoints))
				.withMessageContaining("ObjectNameFactory must not be null");
	}

	@Test
	public void createWhenResponseMapperIsNullShouldThrowException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new JmxEndpointExporter(this.mBeanServer,
						this.objectNameFactory, null, this.endpoints))
				.withMessageContaining("ResponseMapper must not be null");
	}

	@Test
	public void createWhenEndpointsIsNullShouldThrowException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new JmxEndpointExporter(this.mBeanServer,
						this.objectNameFactory, this.responseMapper, null))
				.withMessageContaining("Endpoints must not be null");
	}

	@Test
	public void afterPropertiesSetShouldRegisterMBeans() throws Exception {
		this.endpoints.add(new TestExposableJmxEndpoint(new TestJmxOperation()));
		this.exporter.afterPropertiesSet();
		verify(this.mBeanServer).registerMBean(this.objectCaptor.capture(),
				this.objectNameCaptor.capture());
		assertThat(this.objectCaptor.getValue()).isInstanceOf(EndpointMBean.class);
		assertThat(this.objectNameCaptor.getValue().getKeyProperty("name"))
				.isEqualTo("test");
	}

	@Test
	public void registerShouldUseObjectNameFactory() throws Exception {
		this.endpoints.add(new TestExposableJmxEndpoint(new TestJmxOperation()));
		this.exporter.afterPropertiesSet();
		verify(this.objectNameFactory).getObjectName(any(ExposableJmxEndpoint.class));
	}

	@Test
	public void registerWhenObjectNameIsMalformedShouldThrowException() throws Exception {
		given(this.objectNameFactory.getObjectName(any(ExposableJmxEndpoint.class)))
				.willThrow(MalformedObjectNameException.class);
		this.endpoints.add(new TestExposableJmxEndpoint(new TestJmxOperation()));
		assertThatIllegalStateException().isThrownBy(this.exporter::afterPropertiesSet)
				.withMessageContaining("Invalid ObjectName for endpoint 'test'");
	}

	@Test
	public void registerWhenRegistrationFailsShouldThrowException() throws Exception {
		given(this.mBeanServer.registerMBean(any(), any(ObjectName.class)))
				.willThrow(new MBeanRegistrationException(new RuntimeException()));
		this.endpoints.add(new TestExposableJmxEndpoint(new TestJmxOperation()));
		assertThatExceptionOfType(MBeanExportException.class)
				.isThrownBy(this.exporter::afterPropertiesSet)
				.withMessageContaining("Failed to register MBean for endpoint 'test");
	}

	@Test
	public void destroyShouldUnregisterMBeans() throws Exception {
		this.endpoints.add(new TestExposableJmxEndpoint(new TestJmxOperation()));
		this.exporter.afterPropertiesSet();
		this.exporter.destroy();
		verify(this.mBeanServer).unregisterMBean(this.objectNameCaptor.capture());
		assertThat(this.objectNameCaptor.getValue().getKeyProperty("name"))
				.isEqualTo("test");
	}

	@Test
	public void unregisterWhenInstanceNotFoundShouldContinue() throws Exception {
		this.endpoints.add(new TestExposableJmxEndpoint(new TestJmxOperation()));
		this.exporter.afterPropertiesSet();
		willThrow(InstanceNotFoundException.class).given(this.mBeanServer)
				.unregisterMBean(any(ObjectName.class));
		this.exporter.destroy();
	}

	@Test
	public void unregisterWhenUnregisterThrowsExceptionShouldThrowException()
			throws Exception {
		this.endpoints.add(new TestExposableJmxEndpoint(new TestJmxOperation()));
		this.exporter.afterPropertiesSet();
		willThrow(new MBeanRegistrationException(new RuntimeException()))
				.given(this.mBeanServer).unregisterMBean(any(ObjectName.class));
		assertThatExceptionOfType(JmxException.class)
				.isThrownBy(() -> this.exporter.destroy()).withMessageContaining(
						"Failed to unregister MBean with ObjectName 'boot");
	}

	/**
	 * Test {@link EndpointObjectNameFactory}.
	 */
	private static class TestEndpointObjectNameFactory
			implements EndpointObjectNameFactory {

		@Override
		public ObjectName getObjectName(ExposableJmxEndpoint endpoint)
				throws MalformedObjectNameException {
			return (endpoint != null) ? new ObjectName(
					"boot:type=Endpoint,name=" + endpoint.getEndpointId()) : null;
		}

	}

}
