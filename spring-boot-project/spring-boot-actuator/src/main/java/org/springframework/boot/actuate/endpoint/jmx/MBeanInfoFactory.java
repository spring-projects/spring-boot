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

import java.util.List;

import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.modelmbean.ModelMBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanConstructorInfo;
import javax.management.modelmbean.ModelMBeanInfoSupport;
import javax.management.modelmbean.ModelMBeanNotificationInfo;
import javax.management.modelmbean.ModelMBeanOperationInfo;

import org.springframework.boot.actuate.endpoint.OperationType;

/**
 * Factory to create {@link MBeanInfo} from a {@link ExposableJmxEndpoint}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
class MBeanInfoFactory {

	private static final ModelMBeanAttributeInfo[] NO_ATTRIBUTES = new ModelMBeanAttributeInfo[0];

	private static final ModelMBeanConstructorInfo[] NO_CONSTRUCTORS = new ModelMBeanConstructorInfo[0];

	private static final ModelMBeanNotificationInfo[] NO_NOTIFICATIONS = new ModelMBeanNotificationInfo[0];

	private final JmxOperationResponseMapper responseMapper;

	MBeanInfoFactory(JmxOperationResponseMapper responseMapper) {
		this.responseMapper = responseMapper;
	}

	public MBeanInfo getMBeanInfo(ExposableJmxEndpoint endpoint) {
		String className = EndpointMBean.class.getName();
		String description = getDescription(endpoint);
		ModelMBeanOperationInfo[] operations = getMBeanOperations(endpoint);
		return new ModelMBeanInfoSupport(className, description, NO_ATTRIBUTES,
				NO_CONSTRUCTORS, operations, NO_NOTIFICATIONS);
	}

	private String getDescription(ExposableJmxEndpoint endpoint) {
		return "MBean operations for endpoint " + endpoint.getId();
	}

	private ModelMBeanOperationInfo[] getMBeanOperations(ExposableJmxEndpoint endpoint) {
		return endpoint.getOperations().stream().map(this::getMBeanOperation)
				.toArray(ModelMBeanOperationInfo[]::new);
	}

	private ModelMBeanOperationInfo getMBeanOperation(JmxOperation operation) {
		String name = operation.getName();
		String description = operation.getDescription();
		MBeanParameterInfo[] signature = getSignature(operation.getParameters());
		String type = getType(operation.getOutputType());
		int impact = getImpact(operation.getType());
		return new ModelMBeanOperationInfo(name, description, signature, type, impact);
	}

	private MBeanParameterInfo[] getSignature(List<JmxOperationParameter> parameters) {
		return parameters.stream().map(this::getMBeanParameter)
				.toArray(MBeanParameterInfo[]::new);
	}

	private MBeanParameterInfo getMBeanParameter(JmxOperationParameter parameter) {
		return new MBeanParameterInfo(parameter.getName(), parameter.getType().getName(),
				parameter.getDescription());
	}

	private int getImpact(OperationType operationType) {
		if (operationType == OperationType.READ) {
			return MBeanOperationInfo.INFO;
		}
		if (operationType == OperationType.WRITE
				|| operationType == OperationType.DELETE) {
			return MBeanOperationInfo.ACTION;
		}
		return MBeanOperationInfo.UNKNOWN;
	}

	private String getType(Class<?> outputType) {
		return this.responseMapper.mapResponseType(outputType).getName();
	}

}
