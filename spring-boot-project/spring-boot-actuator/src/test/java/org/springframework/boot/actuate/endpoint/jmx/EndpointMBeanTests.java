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

package org.springframework.boot.actuate.endpoint.jmx;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.ReflectionException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link EndpointMBean}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
public class EndpointMBeanTests {

	private static final Object[] NO_PARAMS = {};

	private static final String[] NO_SIGNATURE = {};

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private TestExposableJmxEndpoint endpoint = new TestExposableJmxEndpoint(
			new TestJmxOperation());

	private TestJmxOperationResponseMapper responseMapper = new TestJmxOperationResponseMapper();

	@Test
	public void createWhenResponseMapperIsNullShouldThrowException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("ResponseMapper must not be null");
		new EndpointMBean(null, mock(ExposableJmxEndpoint.class));
	}

	@Test
	public void createWhenEndpointIsNullShouldThrowException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Endpoint must not be null");
		new EndpointMBean(mock(JmxOperationResponseMapper.class), null);
	}

	@Test
	public void getMBeanInfoShouldReturnMBeanInfo() {
		EndpointMBean bean = new EndpointMBean(this.responseMapper, this.endpoint);
		MBeanInfo info = bean.getMBeanInfo();
		assertThat(info.getDescription()).isEqualTo("MBean operations for endpoint test");
	}

	@Test
	public void invokeShouldInvokeJmxOperation()
			throws MBeanException, ReflectionException {
		EndpointMBean bean = new EndpointMBean(this.responseMapper, this.endpoint);
		Object result = bean.invoke("testOperation", NO_PARAMS, NO_SIGNATURE);
		assertThat(result).isEqualTo("result");
	}

	@Test
	public void invokeWhenActionNameIsNotAnOperationShouldThrowException()
			throws MBeanException, ReflectionException {
		EndpointMBean bean = new EndpointMBean(this.responseMapper, this.endpoint);
		this.thrown.expect(ReflectionException.class);
		this.thrown.expectCause(instanceOf(IllegalArgumentException.class));
		this.thrown.expectMessage("no operation named missingOperation");
		bean.invoke("missingOperation", NO_PARAMS, NO_SIGNATURE);
	}

	@Test
	public void invokeWhenMonoResultShouldBlockOnMono()
			throws MBeanException, ReflectionException {
		TestExposableJmxEndpoint endpoint = new TestExposableJmxEndpoint(
				new TestJmxOperation((arguments) -> Mono.just("monoResult")));
		EndpointMBean bean = new EndpointMBean(this.responseMapper, endpoint);
		Object result = bean.invoke("testOperation", NO_PARAMS, NO_SIGNATURE);
		assertThat(result).isEqualTo("monoResult");
	}

	@Test
	public void invokeShouldCallResponseMapper()
			throws MBeanException, ReflectionException {
		TestJmxOperationResponseMapper responseMapper = spy(this.responseMapper);
		EndpointMBean bean = new EndpointMBean(responseMapper, this.endpoint);
		bean.invoke("testOperation", NO_PARAMS, NO_SIGNATURE);
		verify(responseMapper).mapResponseType(String.class);
		verify(responseMapper).mapResponse("result");
	}

	@Test
	public void getAttributeShouldThrowException()
			throws AttributeNotFoundException, MBeanException, ReflectionException {
		EndpointMBean bean = new EndpointMBean(this.responseMapper, this.endpoint);
		this.thrown.expect(AttributeNotFoundException.class);
		this.thrown.expectMessage("EndpointMBeans do not support attributes");
		bean.getAttribute("test");
	}

	@Test
	public void setAttributeShouldThrowException() throws AttributeNotFoundException,
			InvalidAttributeValueException, MBeanException, ReflectionException {
		EndpointMBean bean = new EndpointMBean(this.responseMapper, this.endpoint);
		this.thrown.expect(AttributeNotFoundException.class);
		this.thrown.expectMessage("EndpointMBeans do not support attributes");
		bean.setAttribute(new Attribute("test", "test"));
	}

	@Test
	public void getAttributesShouldReturnEmptyAttributeList() {
		EndpointMBean bean = new EndpointMBean(this.responseMapper, this.endpoint);
		AttributeList attributes = bean.getAttributes(new String[] { "test" });
		assertThat(attributes).isEmpty();
	}

	@Test
	public void setAttributesShouldReturnEmptyAttributeList() {
		EndpointMBean bean = new EndpointMBean(this.responseMapper, this.endpoint);
		AttributeList sourceAttributes = new AttributeList();
		sourceAttributes.add(new Attribute("test", "test"));
		AttributeList attributes = bean.setAttributes(sourceAttributes);
		assertThat(attributes).isEmpty();
	}

}
