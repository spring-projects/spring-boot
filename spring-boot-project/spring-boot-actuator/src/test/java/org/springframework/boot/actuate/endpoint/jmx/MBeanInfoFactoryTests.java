/*
 * Copyright 2012-2019 the original author or authors.
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

import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.endpoint.OperationType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link MBeanInfoFactory}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
class MBeanInfoFactoryTests {

	private MBeanInfoFactory factory = new MBeanInfoFactory(new TestJmxOperationResponseMapper());

	@Test
	void getMBeanInfoShouldReturnMBeanInfo() {
		MBeanInfo info = this.factory.getMBeanInfo(new TestExposableJmxEndpoint(new TestJmxOperation()));
		assertThat(info).isNotNull();
		assertThat(info.getClassName()).isEqualTo(EndpointMBean.class.getName());
		assertThat(info.getDescription()).isEqualTo("MBean operations for endpoint test");
		assertThat(info.getAttributes()).isEmpty();
		assertThat(info.getNotifications()).isEmpty();
		assertThat(info.getConstructors()).isEmpty();
		assertThat(info.getOperations()).hasSize(1);
		MBeanOperationInfo operationInfo = info.getOperations()[0];
		assertThat(operationInfo.getName()).isEqualTo("testOperation");
		assertThat(operationInfo.getReturnType()).isEqualTo(String.class.getName());
		assertThat(operationInfo.getImpact()).isEqualTo(MBeanOperationInfo.INFO);
		assertThat(operationInfo.getSignature()).hasSize(0);
	}

	@Test
	void getMBeanInfoWhenReadOperationShouldHaveInfoImpact() {
		MBeanInfo info = this.factory
				.getMBeanInfo(new TestExposableJmxEndpoint(new TestJmxOperation(OperationType.READ)));
		assertThat(info.getOperations()[0].getImpact()).isEqualTo(MBeanOperationInfo.INFO);
	}

	@Test
	void getMBeanInfoWhenWriteOperationShouldHaveActionImpact() {
		MBeanInfo info = this.factory
				.getMBeanInfo(new TestExposableJmxEndpoint(new TestJmxOperation(OperationType.WRITE)));
		assertThat(info.getOperations()[0].getImpact()).isEqualTo(MBeanOperationInfo.ACTION);
	}

	@Test
	void getMBeanInfoWhenDeleteOperationShouldHaveActionImpact() {
		MBeanInfo info = this.factory
				.getMBeanInfo(new TestExposableJmxEndpoint(new TestJmxOperation(OperationType.DELETE)));
		assertThat(info.getOperations()[0].getImpact()).isEqualTo(MBeanOperationInfo.ACTION);
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void getMBeanInfoShouldUseJmxOperationResponseMapper() {
		JmxOperationResponseMapper mapper = mock(JmxOperationResponseMapper.class);
		given(mapper.mapResponseType(String.class)).willReturn((Class) Integer.class);
		MBeanInfoFactory factory = new MBeanInfoFactory(mapper);
		MBeanInfo info = factory.getMBeanInfo(new TestExposableJmxEndpoint(new TestJmxOperation()));
		MBeanOperationInfo operationInfo = info.getOperations()[0];
		assertThat(operationInfo.getReturnType()).isEqualTo(Integer.class.getName());
	}

	@Test
	void getMBeanShouldMapOperationParameters() {
		List<JmxOperationParameter> parameters = new ArrayList<>();
		parameters.add(mockParameter("one", String.class, "myone"));
		parameters.add(mockParameter("two", Object.class, null));
		TestJmxOperation operation = new TestJmxOperation(parameters);
		MBeanInfo info = this.factory.getMBeanInfo(new TestExposableJmxEndpoint(operation));
		MBeanOperationInfo operationInfo = info.getOperations()[0];
		MBeanParameterInfo[] signature = operationInfo.getSignature();
		assertThat(signature).hasSize(2);
		assertThat(signature[0].getName()).isEqualTo("one");
		assertThat(signature[0].getType()).isEqualTo(String.class.getName());
		assertThat(signature[0].getDescription()).isEqualTo("myone");
		assertThat(signature[1].getName()).isEqualTo("two");
		assertThat(signature[1].getType()).isEqualTo(Object.class.getName());
		assertThat(signature[1].getDescription()).isNull();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private JmxOperationParameter mockParameter(String name, Class<?> type, String description) {
		JmxOperationParameter parameter = mock(JmxOperationParameter.class);
		given(parameter.getName()).willReturn(name);
		given(parameter.getType()).willReturn((Class) type);
		given(parameter.getDescription()).willReturn(description);
		return parameter;
	}

}
