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

package org.springframework.boot.actuate.autoconfigure.endpoint.jmx;

import java.util.Collections;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.junit.Test;

import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.jmx.ExposableJmxEndpoint;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.util.ObjectUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DefaultEndpointObjectNameFactory}.
 *
 * @author Stephane Nicoll
 */
public class DefaultEndpointObjectNameFactoryTests {

	private final MockEnvironment environment = new MockEnvironment();

	private final JmxEndpointProperties properties = new JmxEndpointProperties(
			this.environment);

	private final MBeanServer mBeanServer = mock(MBeanServer.class);

	private String contextId;

	@Test
	public void generateObjectName() {
		ObjectName objectName = generateObjectName(endpoint(EndpointId.of("test")));
		assertThat(objectName.toString())
				.isEqualTo("org.springframework.boot:type=Endpoint,name=Test");
	}

	@Test
	public void generateObjectNameWithCapitalizedId() {
		ObjectName objectName = generateObjectName(
				endpoint(EndpointId.of("testEndpoint")));
		assertThat(objectName.toString())
				.isEqualTo("org.springframework.boot:type=Endpoint,name=TestEndpoint");
	}

	@Test
	public void generateObjectNameWithCustomDomain() {
		this.properties.setDomain("com.example.acme");
		ObjectName objectName = generateObjectName(endpoint(EndpointId.of("test")));
		assertThat(objectName.toString())
				.isEqualTo("com.example.acme:type=Endpoint,name=Test");
	}

	@Test
	public void generateObjectNameWithUniqueNames() {
		this.environment.setProperty("spring.jmx.unique-names", "true");
		assertUniqueObjectName();
	}

	@Test
	@Deprecated
	public void generateObjectNameWithUniqueNamesDeprecatedProperty() {
		this.properties.setUniqueNames(true);
		assertUniqueObjectName();
	}

	private void assertUniqueObjectName() {
		ExposableJmxEndpoint endpoint = endpoint(EndpointId.of("test"));
		String id = ObjectUtils.getIdentityHexString(endpoint);
		ObjectName objectName = generateObjectName(endpoint);
		assertThat(objectName.toString()).isEqualTo(
				"org.springframework.boot:type=Endpoint,name=Test,identity=" + id);
	}

	@Test
	@Deprecated
	public void generateObjectNameWithUniqueNamesDeprecatedPropertyMismatchMainProperty() {
		this.environment.setProperty("spring.jmx.unique-names", "false");
		this.properties.setUniqueNames(true);
		assertThatIllegalArgumentException()
				.isThrownBy(() -> generateObjectName(endpoint(EndpointId.of("test"))))
				.withMessageContaining("spring.jmx.unique-names")
				.withMessageContaining("management.endpoints.jmx.unique-names");
	}

	@Test
	public void generateObjectNameWithStaticNames() {
		this.properties.getStaticNames().setProperty("counter", "42");
		this.properties.getStaticNames().setProperty("foo", "bar");
		ObjectName objectName = generateObjectName(endpoint(EndpointId.of("test")));
		assertThat(objectName.getKeyProperty("counter")).isEqualTo("42");
		assertThat(objectName.getKeyProperty("foo")).isEqualTo("bar");
		assertThat(objectName.toString())
				.startsWith("org.springframework.boot:type=Endpoint,name=Test,");
	}

	@Test
	public void generateObjectNameWithDuplicate() throws MalformedObjectNameException {
		this.contextId = "testContext";
		given(this.mBeanServer.queryNames(
				new ObjectName("org.springframework.boot:type=Endpoint,name=Test,*"),
				null)).willReturn(
						Collections.singleton(new ObjectName(
								"org.springframework.boot:type=Endpoint,name=Test")));
		ObjectName objectName = generateObjectName(endpoint(EndpointId.of("test")));
		assertThat(objectName.toString()).isEqualTo(
				"org.springframework.boot:type=Endpoint,name=Test,context=testContext");

	}

	private ObjectName generateObjectName(ExposableJmxEndpoint endpoint) {
		try {
			return new DefaultEndpointObjectNameFactory(this.properties, this.environment,
					this.mBeanServer, this.contextId).getObjectName(endpoint);
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
