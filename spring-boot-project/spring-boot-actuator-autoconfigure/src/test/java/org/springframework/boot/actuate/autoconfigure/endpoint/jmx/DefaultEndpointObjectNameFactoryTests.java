/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.endpoint.jmx;

import java.util.Collections;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.jmx.ExposableJmxEndpoint;
import org.springframework.boot.autoconfigure.jmx.JmxProperties;
import org.springframework.util.ObjectUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DefaultEndpointObjectNameFactory}.
 *
 * @author Stephane Nicoll
 */
class DefaultEndpointObjectNameFactoryTests {

	private final JmxEndpointProperties properties = new JmxEndpointProperties();

	private final JmxProperties jmxProperties = new JmxProperties();

	private final MBeanServer mBeanServer = mock(MBeanServer.class);

	private String contextId;

	@Test
	void generateObjectName() {
		ObjectName objectName = generateObjectName(endpoint(EndpointId.of("test")));
		assertThat(objectName.toString()).isEqualTo("org.springframework.boot:type=Endpoint,name=Test");
	}

	@Test
	void generateObjectNameWithCapitalizedId() {
		ObjectName objectName = generateObjectName(endpoint(EndpointId.of("testEndpoint")));
		assertThat(objectName.toString()).isEqualTo("org.springframework.boot:type=Endpoint,name=TestEndpoint");
	}

	@Test
	void generateObjectNameWithCustomDomain() {
		this.properties.setDomain("com.example.acme");
		ObjectName objectName = generateObjectName(endpoint(EndpointId.of("test")));
		assertThat(objectName.toString()).isEqualTo("com.example.acme:type=Endpoint,name=Test");
	}

	@Test
	void generateObjectNameWithUniqueNames() {
		this.jmxProperties.setUniqueNames(true);
		assertUniqueObjectName();
	}

	private void assertUniqueObjectName() {
		ExposableJmxEndpoint endpoint = endpoint(EndpointId.of("test"));
		String id = ObjectUtils.getIdentityHexString(endpoint);
		ObjectName objectName = generateObjectName(endpoint);
		assertThat(objectName.toString()).isEqualTo("org.springframework.boot:type=Endpoint,name=Test,identity=" + id);
	}

	@Test
	void generateObjectNameWithStaticNames() {
		this.properties.getStaticNames().setProperty("counter", "42");
		this.properties.getStaticNames().setProperty("foo", "bar");
		ObjectName objectName = generateObjectName(endpoint(EndpointId.of("test")));
		assertThat(objectName.getKeyProperty("counter")).isEqualTo("42");
		assertThat(objectName.getKeyProperty("foo")).isEqualTo("bar");
		assertThat(objectName.toString()).startsWith("org.springframework.boot:type=Endpoint,name=Test,");
	}

	@Test
	void generateObjectNameWithDuplicate() throws MalformedObjectNameException {
		this.contextId = "testContext";
		given(this.mBeanServer.queryNames(new ObjectName("org.springframework.boot:type=Endpoint,name=Test,*"), null))
				.willReturn(Collections.singleton(new ObjectName("org.springframework.boot:type=Endpoint,name=Test")));
		ObjectName objectName = generateObjectName(endpoint(EndpointId.of("test")));
		assertThat(objectName.toString())
				.isEqualTo("org.springframework.boot:type=Endpoint,name=Test,context=testContext");

	}

	private ObjectName generateObjectName(ExposableJmxEndpoint endpoint) {
		try {
			return new DefaultEndpointObjectNameFactory(this.properties, this.jmxProperties, this.mBeanServer,
					this.contextId).getObjectName(endpoint);
		}
		catch (MalformedObjectNameException ex) {
			throw new AssertionError("Invalid object name", ex);
		}
	}

	private ExposableJmxEndpoint endpoint(EndpointId id) {
		ExposableJmxEndpoint endpoint = mock(ExposableJmxEndpoint.class);
		given(endpoint.getEndpointId()).willReturn(id);
		return endpoint;
	}

}
