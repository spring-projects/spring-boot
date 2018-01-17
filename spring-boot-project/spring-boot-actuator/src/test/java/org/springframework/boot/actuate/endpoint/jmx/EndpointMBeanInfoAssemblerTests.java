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

import java.util.Collections;
import java.util.Map;

import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;

import org.junit.Test;

import org.springframework.boot.actuate.endpoint.EndpointInfo;
import org.springframework.boot.actuate.endpoint.OperationInvoker;
import org.springframework.boot.actuate.endpoint.OperationType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link EndpointMBeanInfoAssembler}.
 *
 * @author Stephane Nicoll
 */
public class EndpointMBeanInfoAssemblerTests {

	private final EndpointMBeanInfoAssembler mBeanInfoAssembler = new EndpointMBeanInfoAssembler(
			new DummyOperationResponseMapper());

	@Test
	public void exposeSimpleReadOperation() {
		JmxOperation operation = new JmxOperation(OperationType.READ,
				new DummyOperationInvoker(), "getAll", Object.class, "Test operation",
				Collections.emptyList());
		EndpointInfo<JmxOperation> endpoint = new EndpointInfo<>("test", true,
				Collections.singletonList(operation));
		EndpointMBeanInfo endpointMBeanInfo = this.mBeanInfoAssembler
				.createEndpointMBeanInfo(endpoint);
		assertThat(endpointMBeanInfo).isNotNull();
		assertThat(endpointMBeanInfo.getEndpointId()).isEqualTo("test");
		assertThat(endpointMBeanInfo.getOperations())
				.containsOnly(entry("getAll", operation));
		MBeanInfo mbeanInfo = endpointMBeanInfo.getMbeanInfo();
		assertThat(mbeanInfo).isNotNull();
		assertThat(mbeanInfo.getClassName()).isEqualTo(EndpointMBean.class.getName());
		assertThat(mbeanInfo.getDescription())
				.isEqualTo("MBean operations for endpoint test");
		assertThat(mbeanInfo.getAttributes()).isEmpty();
		assertThat(mbeanInfo.getNotifications()).isEmpty();
		assertThat(mbeanInfo.getConstructors()).isEmpty();
		assertThat(mbeanInfo.getOperations()).hasSize(1);
		MBeanOperationInfo mBeanOperationInfo = mbeanInfo.getOperations()[0];
		assertThat(mBeanOperationInfo.getName()).isEqualTo("getAll");
		assertThat(mBeanOperationInfo.getReturnType()).isEqualTo(Object.class.getName());
		assertThat(mBeanOperationInfo.getImpact()).isEqualTo(MBeanOperationInfo.INFO);
		assertThat(mBeanOperationInfo.getSignature()).hasSize(0);
	}

	@Test
	public void exposeSimpleWriteOperation() {
		JmxOperation operation = new JmxOperation(OperationType.WRITE,
				new DummyOperationInvoker(), "update", Object.class, "Update operation",
				Collections.singletonList(new JmxEndpointOperationParameterInfo("test",
						String.class, "Test argument")));
		EndpointInfo<JmxOperation> endpoint = new EndpointInfo<>("another", true,
				Collections.singletonList(operation));
		EndpointMBeanInfo endpointMBeanInfo = this.mBeanInfoAssembler
				.createEndpointMBeanInfo(endpoint);
		assertThat(endpointMBeanInfo).isNotNull();
		assertThat(endpointMBeanInfo.getEndpointId()).isEqualTo("another");
		assertThat(endpointMBeanInfo.getOperations())
				.containsOnly(entry("update", operation));
		MBeanInfo mbeanInfo = endpointMBeanInfo.getMbeanInfo();
		assertThat(mbeanInfo).isNotNull();
		assertThat(mbeanInfo.getClassName()).isEqualTo(EndpointMBean.class.getName());
		assertThat(mbeanInfo.getDescription())
				.isEqualTo("MBean operations for endpoint another");
		assertThat(mbeanInfo.getAttributes()).isEmpty();
		assertThat(mbeanInfo.getNotifications()).isEmpty();
		assertThat(mbeanInfo.getConstructors()).isEmpty();
		assertThat(mbeanInfo.getOperations()).hasSize(1);
		MBeanOperationInfo mBeanOperationInfo = mbeanInfo.getOperations()[0];
		assertThat(mBeanOperationInfo.getName()).isEqualTo("update");
		assertThat(mBeanOperationInfo.getReturnType()).isEqualTo(Object.class.getName());
		assertThat(mBeanOperationInfo.getImpact()).isEqualTo(MBeanOperationInfo.ACTION);
		assertThat(mBeanOperationInfo.getSignature()).hasSize(1);
		MBeanParameterInfo mBeanParameterInfo = mBeanOperationInfo.getSignature()[0];
		assertThat(mBeanParameterInfo.getName()).isEqualTo("test");
		assertThat(mBeanParameterInfo.getType()).isEqualTo(String.class.getName());
		assertThat(mBeanParameterInfo.getDescription()).isEqualTo("Test argument");
	}

	private static class DummyOperationInvoker implements OperationInvoker {

		@Override
		public Object invoke(Map<String, Object> arguments) {
			return null;
		}

	}

	private static class DummyOperationResponseMapper
			implements JmxOperationResponseMapper {

		@Override
		public Object mapResponse(Object response) {
			return response;
		}

		@Override
		public Class<?> mapResponseType(Class<?> responseType) {
			return responseType;
		}
	}

}
