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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.modelmbean.ModelMBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanConstructorInfo;
import javax.management.modelmbean.ModelMBeanInfoSupport;
import javax.management.modelmbean.ModelMBeanNotificationInfo;
import javax.management.modelmbean.ModelMBeanOperationInfo;

import org.springframework.boot.actuate.endpoint.EndpointInfo;
import org.springframework.boot.actuate.endpoint.OperationType;

/**
 * Gathers the management operations of a particular {@link EndpointInfo endpoint}.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 */
class EndpointMBeanInfoAssembler {

	private final JmxOperationResponseMapper responseMapper;

	EndpointMBeanInfoAssembler(JmxOperationResponseMapper responseMapper) {
		this.responseMapper = responseMapper;
	}

	/**
	 * Creates the {@link EndpointMBeanInfo} for the specified {@link EndpointInfo
	 * endpoint}.
	 * @param endpointInfo the endpoint to handle
	 * @return the mbean info for the endpoint
	 */
	EndpointMBeanInfo createEndpointMBeanInfo(
			EndpointInfo<JmxEndpointOperation> endpointInfo) {
		Map<String, OperationInfos> operationsMapping = getOperationInfo(endpointInfo);
		ModelMBeanOperationInfo[] operationsMBeanInfo = operationsMapping.values()
				.stream().map((t) -> t.mBeanOperationInfo).collect(Collectors.toList())
				.toArray(new ModelMBeanOperationInfo[] {});
		Map<String, JmxEndpointOperation> operationsInfo = new LinkedHashMap<>();
		operationsMapping.forEach((name, t) -> operationsInfo.put(name, t.operation));
		MBeanInfo info = new ModelMBeanInfoSupport(EndpointMBean.class.getName(),
				getDescription(endpointInfo), new ModelMBeanAttributeInfo[0],
				new ModelMBeanConstructorInfo[0], operationsMBeanInfo,
				new ModelMBeanNotificationInfo[0]);
		return new EndpointMBeanInfo(endpointInfo.getId(), info, operationsInfo);
	}

	private String getDescription(EndpointInfo<?> endpointInfo) {
		return "MBean operations for endpoint " + endpointInfo.getId();
	}

	private Map<String, OperationInfos> getOperationInfo(
			EndpointInfo<JmxEndpointOperation> endpointInfo) {
		Map<String, OperationInfos> operationInfos = new HashMap<>();
		endpointInfo.getOperations().forEach((operationInfo) -> {
			String name = operationInfo.getOperationName();
			ModelMBeanOperationInfo mBeanOperationInfo = new ModelMBeanOperationInfo(
					operationInfo.getOperationName(), operationInfo.getDescription(),
					getMBeanParameterInfos(operationInfo), this.responseMapper
							.mapResponseType(operationInfo.getOutputType()).getName(),
					mapOperationType(operationInfo.getType()));
			operationInfos.put(name,
					new OperationInfos(mBeanOperationInfo, operationInfo));
		});
		return operationInfos;
	}

	private MBeanParameterInfo[] getMBeanParameterInfos(JmxEndpointOperation operation) {
		return operation.getParameters().stream()
				.map((operationParameter) -> new MBeanParameterInfo(
						operationParameter.getName(),
						operationParameter.getType().getName(),
						operationParameter.getDescription()))
				.collect(Collectors.collectingAndThen(Collectors.toList(),
						(parameterInfos) -> parameterInfos
								.toArray(new MBeanParameterInfo[parameterInfos.size()])));
	}

	private int mapOperationType(OperationType type) {
		if (type == OperationType.READ) {
			return MBeanOperationInfo.INFO;
		}
		if (type == OperationType.WRITE || type == OperationType.DELETE) {
			return MBeanOperationInfo.ACTION;
		}
		return MBeanOperationInfo.UNKNOWN;
	}

	private static class OperationInfos {

		private final ModelMBeanOperationInfo mBeanOperationInfo;

		private final JmxEndpointOperation operation;

		OperationInfos(ModelMBeanOperationInfo mBeanOperationInfo,
				JmxEndpointOperation operation) {
			this.mBeanOperationInfo = mBeanOperationInfo;
			this.operation = operation;
		}

	}

}
