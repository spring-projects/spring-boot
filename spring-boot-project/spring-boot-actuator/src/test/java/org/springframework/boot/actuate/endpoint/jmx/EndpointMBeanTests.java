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

import java.net.URL;
import java.net.URLClassLoader;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.ReflectionException;

import org.junit.Test;
import reactor.core.publisher.Mono;

import org.springframework.beans.FatalBeanException;
import org.springframework.boot.actuate.endpoint.InvalidEndpointRequestException;
import org.springframework.boot.actuate.endpoint.InvocationContext;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
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

	private TestExposableJmxEndpoint endpoint = new TestExposableJmxEndpoint(
			new TestJmxOperation());

	private TestJmxOperationResponseMapper responseMapper = new TestJmxOperationResponseMapper();

	@Test
	public void createWhenResponseMapperIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> new EndpointMBean(null, null, mock(ExposableJmxEndpoint.class)))
				.withMessageContaining("ResponseMapper must not be null");
	}

	@Test
	public void createWhenEndpointIsNullShouldThrowException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new EndpointMBean(
						mock(JmxOperationResponseMapper.class), null, null))
				.withMessageContaining("Endpoint must not be null");
	}

	@Test
	public void getMBeanInfoShouldReturnMBeanInfo() {
		EndpointMBean bean = createEndpointMBean();
		MBeanInfo info = bean.getMBeanInfo();
		assertThat(info.getDescription()).isEqualTo("MBean operations for endpoint test");
	}

	@Test
	public void invokeShouldInvokeJmxOperation()
			throws MBeanException, ReflectionException {
		EndpointMBean bean = createEndpointMBean();
		Object result = bean.invoke("testOperation", NO_PARAMS, NO_SIGNATURE);
		assertThat(result).isEqualTo("result");
	}

	@Test
	public void invokeWhenOperationFailedShouldTranslateException()
			throws MBeanException, ReflectionException {
		TestExposableJmxEndpoint endpoint = new TestExposableJmxEndpoint(
				new TestJmxOperation((arguments) -> {
					throw new FatalBeanException("test failure");
				}));
		EndpointMBean bean = new EndpointMBean(this.responseMapper, null, endpoint);
		assertThatExceptionOfType(MBeanException.class)
				.isThrownBy(() -> bean.invoke("testOperation", NO_PARAMS, NO_SIGNATURE))
				.withCauseInstanceOf(IllegalStateException.class)
				.withMessageContaining("test failure");

	}

	@Test
	public void invokeWhenOperationFailedWithJdkExceptionShouldReuseException()
			throws MBeanException, ReflectionException {
		TestExposableJmxEndpoint endpoint = new TestExposableJmxEndpoint(
				new TestJmxOperation((arguments) -> {
					throw new UnsupportedOperationException("test failure");
				}));
		EndpointMBean bean = new EndpointMBean(this.responseMapper, null, endpoint);
		assertThatExceptionOfType(MBeanException.class)
				.isThrownBy(() -> bean.invoke("testOperation", NO_PARAMS, NO_SIGNATURE))
				.withCauseInstanceOf(UnsupportedOperationException.class)
				.withMessageContaining("test failure");
	}

	@Test
	public void invokeWhenActionNameIsNotAnOperationShouldThrowException()
			throws MBeanException, ReflectionException {
		EndpointMBean bean = createEndpointMBean();
		assertThatExceptionOfType(ReflectionException.class)
				.isThrownBy(
						() -> bean.invoke("missingOperation", NO_PARAMS, NO_SIGNATURE))
				.withCauseInstanceOf(IllegalArgumentException.class)
				.withMessageContaining("no operation named missingOperation");
	}

	@Test
	public void invokeShouldInvokeJmxOperationWithBeanClassLoader()
			throws ReflectionException, MBeanException {
		ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
		TestExposableJmxEndpoint endpoint = new TestExposableJmxEndpoint(
				new TestJmxOperation((arguments) -> ClassUtils.getDefaultClassLoader()));
		URLClassLoader beanClassLoader = new URLClassLoader(new URL[0],
				getClass().getClassLoader());
		EndpointMBean bean = new EndpointMBean(this.responseMapper, beanClassLoader,
				endpoint);
		Object result = bean.invoke("testOperation", NO_PARAMS, NO_SIGNATURE);
		assertThat(result).isEqualTo(beanClassLoader);
		assertThat(Thread.currentThread().getContextClassLoader())
				.isEqualTo(originalClassLoader);
	}

	@Test
	public void invokeWhenOperationIsInvalidShouldThrowException()
			throws MBeanException, ReflectionException {
		TestJmxOperation operation = new TestJmxOperation() {

			@Override
			public Object invoke(InvocationContext context) {
				throw new InvalidEndpointRequestException("test failure", "test");
			}

		};
		TestExposableJmxEndpoint endpoint = new TestExposableJmxEndpoint(operation);
		EndpointMBean bean = new EndpointMBean(this.responseMapper, null, endpoint);
		assertThatExceptionOfType(ReflectionException.class)
				.isThrownBy(() -> bean.invoke("testOperation", NO_PARAMS, NO_SIGNATURE))
				.withRootCauseInstanceOf(IllegalArgumentException.class)
				.withMessageContaining("test failure");
	}

	@Test
	public void invokeWhenMonoResultShouldBlockOnMono()
			throws MBeanException, ReflectionException {
		TestExposableJmxEndpoint endpoint = new TestExposableJmxEndpoint(
				new TestJmxOperation((arguments) -> Mono.just("monoResult")));
		EndpointMBean bean = new EndpointMBean(this.responseMapper, null, endpoint);
		Object result = bean.invoke("testOperation", NO_PARAMS, NO_SIGNATURE);
		assertThat(result).isEqualTo("monoResult");
	}

	@Test
	public void invokeShouldCallResponseMapper()
			throws MBeanException, ReflectionException {
		TestJmxOperationResponseMapper responseMapper = spy(this.responseMapper);
		EndpointMBean bean = new EndpointMBean(responseMapper, null, this.endpoint);
		bean.invoke("testOperation", NO_PARAMS, NO_SIGNATURE);
		verify(responseMapper).mapResponseType(String.class);
		verify(responseMapper).mapResponse("result");
	}

	@Test
	public void getAttributeShouldThrowException()
			throws AttributeNotFoundException, MBeanException, ReflectionException {
		EndpointMBean bean = createEndpointMBean();
		assertThatExceptionOfType(AttributeNotFoundException.class)
				.isThrownBy(() -> bean.getAttribute("test"))
				.withMessageContaining("EndpointMBeans do not support attributes");
	}

	@Test
	public void setAttributeShouldThrowException() throws AttributeNotFoundException,
			InvalidAttributeValueException, MBeanException, ReflectionException {
		EndpointMBean bean = createEndpointMBean();
		assertThatExceptionOfType(AttributeNotFoundException.class)
				.isThrownBy(() -> bean.setAttribute(new Attribute("test", "test")))
				.withMessageContaining("EndpointMBeans do not support attributes");
	}

	@Test
	public void getAttributesShouldReturnEmptyAttributeList() {
		EndpointMBean bean = createEndpointMBean();
		AttributeList attributes = bean.getAttributes(new String[] { "test" });
		assertThat(attributes).isEmpty();
	}

	@Test
	public void setAttributesShouldReturnEmptyAttributeList() {
		EndpointMBean bean = createEndpointMBean();
		AttributeList sourceAttributes = new AttributeList();
		sourceAttributes.add(new Attribute("test", "test"));
		AttributeList attributes = bean.setAttributes(sourceAttributes);
		assertThat(attributes).isEmpty();
	}

	private EndpointMBean createEndpointMBean() {
		return new EndpointMBean(this.responseMapper, null, this.endpoint);
	}

}
