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

import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.jmx.JmxException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link EndpointMBeanRegistrar}.
 *
 * @author Stephane Nicoll
 */
public class EndpointMBeanRegistrarTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private MBeanServer mBeanServer = mock(MBeanServer.class);

	@Test
	public void mBeanServerMustNotBeNull() {
		this.thrown.expect(IllegalArgumentException.class);
		new EndpointMBeanRegistrar(null, (e) -> new ObjectName("foo"));
	}

	@Test
	public void objectNameFactoryMustNotBeNull() {
		this.thrown.expect(IllegalArgumentException.class);
		new EndpointMBeanRegistrar(this.mBeanServer, null);
	}

	@Test
	public void endpointMustNotBeNull() {
		EndpointMBeanRegistrar registrar = new EndpointMBeanRegistrar(this.mBeanServer,
				(e) -> new ObjectName("foo"));
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Endpoint must not be null");
		registrar.registerEndpointMBean(null);
	}

	@Test
	public void registerEndpointInvokesObjectNameFactory()
			throws MalformedObjectNameException {
		EndpointObjectNameFactory factory = mock(EndpointObjectNameFactory.class);
		EndpointMBean endpointMBean = mock(EndpointMBean.class);
		ObjectName objectName = mock(ObjectName.class);
		given(factory.generate(endpointMBean)).willReturn(objectName);
		EndpointMBeanRegistrar registrar = new EndpointMBeanRegistrar(this.mBeanServer,
				factory);
		ObjectName actualObjectName = registrar.registerEndpointMBean(endpointMBean);
		assertThat(actualObjectName).isSameAs(objectName);
		verify(factory).generate(endpointMBean);
	}

	@Test
	public void registerEndpointInvalidObjectName() throws MalformedObjectNameException {
		EndpointMBean endpointMBean = mock(EndpointMBean.class);
		given(endpointMBean.getEndpointId()).willReturn("test");
		EndpointObjectNameFactory factory = mock(EndpointObjectNameFactory.class);
		given(factory.generate(endpointMBean))
				.willThrow(new MalformedObjectNameException());
		EndpointMBeanRegistrar registrar = new EndpointMBeanRegistrar(this.mBeanServer,
				factory);
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("Invalid ObjectName for endpoint with id 'test'");
		registrar.registerEndpointMBean(endpointMBean);
	}

	@Test
	public void registerEndpointFailure() throws Exception {
		EndpointMBean endpointMBean = mock(EndpointMBean.class);
		given(endpointMBean.getEndpointId()).willReturn("test");
		EndpointObjectNameFactory factory = mock(EndpointObjectNameFactory.class);
		ObjectName objectName = mock(ObjectName.class);
		given(factory.generate(endpointMBean)).willReturn(objectName);
		given(this.mBeanServer.registerMBean(endpointMBean, objectName))
				.willThrow(MBeanRegistrationException.class);
		EndpointMBeanRegistrar registrar = new EndpointMBeanRegistrar(this.mBeanServer,
				factory);
		this.thrown.expect(JmxException.class);
		this.thrown.expectMessage("Failed to register MBean for endpoint with id 'test'");
		registrar.registerEndpointMBean(endpointMBean);
	}

	@Test
	public void unregisterEndpoint() throws Exception {
		ObjectName objectName = mock(ObjectName.class);
		EndpointMBeanRegistrar registrar = new EndpointMBeanRegistrar(this.mBeanServer,
				mock(EndpointObjectNameFactory.class));
		assertThat(registrar.unregisterEndpointMbean(objectName)).isTrue();
		verify(this.mBeanServer).unregisterMBean(objectName);
	}

	@Test
	public void unregisterUnknownEndpoint() throws Exception {
		ObjectName objectName = mock(ObjectName.class);
		willThrow(InstanceNotFoundException.class).given(this.mBeanServer)
				.unregisterMBean(objectName);
		EndpointMBeanRegistrar registrar = new EndpointMBeanRegistrar(this.mBeanServer,
				mock(EndpointObjectNameFactory.class));
		assertThat(registrar.unregisterEndpointMbean(objectName)).isFalse();
		verify(this.mBeanServer).unregisterMBean(objectName);
	}

}
