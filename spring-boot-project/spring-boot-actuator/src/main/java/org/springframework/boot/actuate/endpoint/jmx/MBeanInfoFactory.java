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
 * Factory to create {@link MBeanInfo} from an {@link ExposableJmxEndpoint}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
class MBeanInfoFactory {

	private static final ModelMBeanAttributeInfo[] NO_ATTRIBUTES = new ModelMBeanAttributeInfo[0];

	private static final ModelMBeanConstructorInfo[] NO_CONSTRUCTORS = new ModelMBeanConstructorInfo[0];

	private static final ModelMBeanNotificationInfo[] NO_NOTIFICATIONS = new ModelMBeanNotificationInfo[0];

	private final JmxOperationResponseMapper responseMapper;

	/**
     * Constructs a new instance of MBeanInfoFactory with the specified JmxOperationResponseMapper.
     *
     * @param responseMapper the JmxOperationResponseMapper to be used by this MBeanInfoFactory
     */
    MBeanInfoFactory(JmxOperationResponseMapper responseMapper) {
		this.responseMapper = responseMapper;
	}

	/**
     * Retrieves the MBeanInfo for the given ExposableJmxEndpoint.
     * 
     * @param endpoint the ExposableJmxEndpoint for which to retrieve the MBeanInfo
     * @return the MBeanInfo object containing information about the MBean
     */
    MBeanInfo getMBeanInfo(ExposableJmxEndpoint endpoint) {
		String className = EndpointMBean.class.getName();
		String description = getDescription(endpoint);
		ModelMBeanOperationInfo[] operations = getMBeanOperations(endpoint);
		return new ModelMBeanInfoSupport(className, description, NO_ATTRIBUTES, NO_CONSTRUCTORS, operations,
				NO_NOTIFICATIONS);
	}

	/**
     * Returns the description of the MBean operations for the specified endpoint.
     *
     * @param endpoint the ExposableJmxEndpoint object representing the endpoint
     * @return the description of the MBean operations for the endpoint
     */
    private String getDescription(ExposableJmxEndpoint endpoint) {
		return "MBean operations for endpoint " + endpoint.getEndpointId();
	}

	/**
     * Retrieves the MBean operations from the given ExposableJmxEndpoint and converts them into an array of ModelMBeanOperationInfo objects.
     * 
     * @param endpoint the ExposableJmxEndpoint from which to retrieve the operations
     * @return an array of ModelMBeanOperationInfo objects representing the MBean operations
     */
    private ModelMBeanOperationInfo[] getMBeanOperations(ExposableJmxEndpoint endpoint) {
		return endpoint.getOperations().stream().map(this::getMBeanOperation).toArray(ModelMBeanOperationInfo[]::new);
	}

	/**
     * Returns a ModelMBeanOperationInfo object based on the provided JmxOperation object.
     * 
     * @param operation the JmxOperation object representing the operation
     * @return a ModelMBeanOperationInfo object representing the operation
     */
    private ModelMBeanOperationInfo getMBeanOperation(JmxOperation operation) {
		String name = operation.getName();
		String description = operation.getDescription();
		MBeanParameterInfo[] signature = getSignature(operation.getParameters());
		String type = getType(operation.getOutputType());
		int impact = getImpact(operation.getType());
		return new ModelMBeanOperationInfo(name, description, signature, type, impact);
	}

	/**
     * Returns an array of MBeanParameterInfo objects representing the signature of the operation.
     *
     * @param parameters the list of JmxOperationParameter objects
     * @return an array of MBeanParameterInfo objects representing the signature of the operation
     */
    private MBeanParameterInfo[] getSignature(List<JmxOperationParameter> parameters) {
		return parameters.stream().map(this::getMBeanParameter).toArray(MBeanParameterInfo[]::new);
	}

	/**
     * Returns an instance of MBeanParameterInfo based on the provided JmxOperationParameter.
     * 
     * @param parameter the JmxOperationParameter object to create MBeanParameterInfo from
     * @return an instance of MBeanParameterInfo with the specified name, type, and description
     */
    private MBeanParameterInfo getMBeanParameter(JmxOperationParameter parameter) {
		return new MBeanParameterInfo(parameter.getName(), parameter.getType().getName(), parameter.getDescription());
	}

	/**
     * Returns the impact of the operation based on the given operation type.
     * 
     * @param operationType the type of operation
     * @return the impact of the operation
     *         <ul>
     *         <li>{@link MBeanOperationInfo#INFO} if the operation type is {@link OperationType#READ}</li>
     *         <li>{@link MBeanOperationInfo#ACTION} if the operation type is {@link OperationType#WRITE} or {@link OperationType#DELETE}</li>
     *         <li>{@link MBeanOperationInfo#UNKNOWN} if the operation type is unknown</li>
     *         </ul>
     */
    private int getImpact(OperationType operationType) {
		if (operationType == OperationType.READ) {
			return MBeanOperationInfo.INFO;
		}
		if (operationType == OperationType.WRITE || operationType == OperationType.DELETE) {
			return MBeanOperationInfo.ACTION;
		}
		return MBeanOperationInfo.UNKNOWN;
	}

	/**
     * Returns the type of the output based on the provided outputType.
     * 
     * @param outputType the class representing the output type
     * @return the name of the output type
     */
    private String getType(Class<?> outputType) {
		return this.responseMapper.mapResponseType(outputType).getName();
	}

}
