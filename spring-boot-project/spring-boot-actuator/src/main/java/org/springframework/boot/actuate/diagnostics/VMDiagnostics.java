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

package org.springframework.boot.actuate.diagnostics;

import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

import javax.management.Descriptor;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanException;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Expose diagnostics information from DiagnosticCommandMBean.
 *
 * @author Luis De Bello
 * @since 2.2.0
 */
public final class VMDiagnostics {

	/**
	 * Error message for get operations.
	 */
	static final String GET_OPERATIONS_ERROR_MESSAGE = "Unable to get operations";

	/**
	 * Error message for no help available.
	 */
	static final String NO_HELP_AVAILABLE_ERROR_MESSAGE = "No help available";

	private static final Log logger = LogFactory.getLog(VMDiagnostics.class);

	/**
	 * Object Name of DiagnosticCommandMBean.
	 */
	private static final String DIAGNOSTIC_COMMAND_MBEAN_OBJECT_NAME = "com.sun.management:type=DiagnosticCommand";

	/**
	 * Field name for help in the operation descriptor.
	 */
	private static final String HELP_FIELD_OPERATION_DESCRIPTOR = "dcmd.help";

	/**
	 * Field name for arguments in the operation descriptor.
	 */
	private static final String ARGUMENTS_FIELD_OPERATION_DESCRIPTOR = "dcmd.arguments";

	/**
	 * Argument name.
	 */
	private static final String ARGUMENT_NAME_DESCRIPTOR = "dcmd.arg.name";

	/**
	 * Argument type.
	 */
	private static final String ARGUMENT_TYPE_DESCRIPTOR = "dcmd.arg.type";

	/**
	 * Argument position.
	 */
	private static final String ARGUMENT_POSITION_DESCRIPTOR = "dcmd.arg.position";

	/**
	 * Platform MBean Server.
	 */
	private final MBeanServer server = ManagementFactory.getPlatformMBeanServer();

	/**
	 * Object Name.
	 */
	private final ObjectName objectName;

	/**
	 * create an instance of diagnostics with the provided object name.
	 * @param objectName objectName associated with DiagnosticCommand MBean.
	 */
	private VMDiagnostics(ObjectName objectName) {
		this.objectName = objectName;
	}

	/**
	 * factory method to {@link VMDiagnostics}.
	 * @return a new instance of {@link VMDiagnostics}.
	 */
	public static VMDiagnostics newInstance() {
		try {
			return new VMDiagnostics(new ObjectName(DIAGNOSTIC_COMMAND_MBEAN_OBJECT_NAME));
		}
		catch (MalformedObjectNameException ex) {
			logger.info("Unable to create VMDiagnostics", ex);
			throw new RuntimeException("Unable to create VMDiagnostics", ex);
		}
	}

	/**
	 * list operations.
	 * @return a list of operations.
	 */
	public String getOperations() {
		StringJoiner result = new StringJoiner(System.lineSeparator());
		try {
			MBeanOperationInfo[] operations = this.server.getMBeanInfo(this.objectName).getOperations();
			for (MBeanOperationInfo operation : operations) {
				result.add(operation.getName());
			}
		}
		catch (InstanceNotFoundException | IntrospectionException | ReflectionException ex) {
			logger.info(GET_OPERATIONS_ERROR_MESSAGE, ex);
			result.add(String.format("%s: %s", GET_OPERATIONS_ERROR_MESSAGE, ex.getMessage()));
		}

		return result.toString();
	}

	/**
	 * get operation details.
	 * @param operation operation name to get details.
	 * @return the details for this operation.
	 */
	public String getOperationDetails(String operation) {
		return getOperationDescriptor(operation).map(this::extractOperationHelp)
				.orElse(NO_HELP_AVAILABLE_ERROR_MESSAGE);
	}

	/**
	 * execute an operation with optional parameters.
	 * @param operation operation name to execute.
	 * @param parameters parameters to the operation.
	 * @return the result of the operation
	 */
	public String executeOperation(String operation, Map<String, String> parameters) {
		return getOperationDescriptor(operation)
				.map((operationDescriptor) -> executeOperation(operation, parameters, operationDescriptor))
				.orElse("Operation cannot be executed");
	}

	/**
	 * get operation descriptor based on operation name.
	 * @param operation name.
	 * @return the operation descriptor.
	 */
	private Optional<Descriptor> getOperationDescriptor(String operation) {
		Optional<Descriptor> maybeOperationDescriptor = Optional.empty();

		try {
			MBeanOperationInfo[] operations = this.server.getMBeanInfo(this.objectName).getOperations();
			for (MBeanOperationInfo currentOperation : operations) {
				if (currentOperation.getName().equals(operation)) {
					Descriptor operationDescriptor = currentOperation.getDescriptor();
					maybeOperationDescriptor = Optional.of(operationDescriptor);
				}
			}
		}
		catch (InstanceNotFoundException | IntrospectionException | ReflectionException ex) {
			logger.info("Operation Descriptor Not Found", ex);

		}
		return maybeOperationDescriptor;
	}

	/**
	 * get operation help based on operation descriptor.
	 * @param operationDescriptor descriptor.
	 * @return help for the operation.
	 */
	private String extractOperationHelp(Descriptor operationDescriptor) {
		Object operationHelp = operationDescriptor.getFieldValue(HELP_FIELD_OPERATION_DESCRIPTOR);

		return (operationHelp != null) ? operationHelp.toString() : NO_HELP_AVAILABLE_ERROR_MESSAGE;
	}

	/**
	 * execute a operation with the provided parameters.
	 * @param operation operation name.
	 * @param parameters parameters.
	 * @param operationDescriptor descriptor.
	 * @return result for the operation.
	 */
	private String executeOperation(String operation, Map<String, String> parameters, Descriptor operationDescriptor) {
		String result;
		try {
			Object[] params = buildOperationParameters(parameters, operationDescriptor);

			result = (String) this.server.invoke(this.objectName, operation, params,
					new String[] { String[].class.getName() });
		}
		catch (ReflectionException | InstanceNotFoundException | MBeanException ex) {
			logger.info(String.format("Unable to access operation: %s", operation), ex);
			result = String.format("Unable to access operation: '%s' - %s", operation, ex.getMessage());
		}
		return result;
	}

	/**
	 * build operation parameters wrapped in a array of objects.
	 * @param parameters parameters.
	 * @param operationDescriptor descriptor.
	 * @return object array to use.
	 */
	private Object[] buildOperationParameters(Map<String, String> parameters, Descriptor operationDescriptor) {
		Optional<Descriptor> maybeArgumentsDescriptor = Optional
				.ofNullable((Descriptor) operationDescriptor.getFieldValue(ARGUMENTS_FIELD_OPERATION_DESCRIPTOR));
		return new Object[] { maybeArgumentsDescriptor
				.map((argumentsDescriptor) -> buildParameters(parameters, argumentsDescriptor)).orElse(null) };

	}

	/**
	 * build operation parameters as array of strings.
	 * @param parameters parameters.
	 * @param argumentsDescriptor descriptor.
	 * @return parameters as array of strings.
	 */
	private String[] buildParameters(Map<String, String> parameters, Descriptor argumentsDescriptor) {
		String[] argumentNames = argumentsDescriptor.getFieldNames();
		String[] values = new String[argumentNames.length];

		if (parameters != null) {
			for (Map.Entry<String, String> entry : parameters.entrySet()) {
				String parameterName = entry.getKey();
				String parameterValue = entry.getValue();

				Descriptor parameterDescriptor = (Descriptor) argumentsDescriptor.getFieldValue(parameterName);
				if (parameterDescriptor != null) {
					values[computeParameterPosition(parameterDescriptor)] = computeParameterValue(parameterDescriptor,
							parameterValue);
				}
				else {
					logger.info(String.format("Invalid parameter: %s", parameterName));
				}
			}
		}

		return removeTrailingNullValues(values);
	}

	/**
	 * remove trailing null for parameters.
	 * @param values array of parameter values.
	 * @return parameter values without trailing nulls.
	 */
	private String[] removeTrailingNullValues(String[] values) {
		int trailingNulls = 0;
		for (int i = values.length - 1; i >= 0; i--) {
			if (values[i] == null) {
				trailingNulls++;
			}
			else {
				break;
			}
		}

		if (trailingNulls == 0) {
			return values;
		}
		else if (values.length == trailingNulls) {
			return null;
		}
		else {
			int newSize = values.length - trailingNulls;
			String[] newValues = new String[newSize];
			System.arraycopy(values, 0, newValues, 0, newSize);

			return newValues;
		}
	}

	/**
	 * calculate parameter position.
	 * @param parameterDescriptor descriptor.
	 * @return the index based on the parameter descriptor.
	 */
	private int computeParameterPosition(Descriptor parameterDescriptor) {
		Integer parameterPosition = (Integer) parameterDescriptor.getFieldValue(ARGUMENT_POSITION_DESCRIPTOR);
		return (parameterPosition != -1) ? parameterPosition : 0;
	}

	/**
	 * calculate parameter value based on parameters.
	 * @param parameterDescriptor descriptor.
	 * @param parameterValue value.
	 * @return value.
	 */
	private String computeParameterValue(Descriptor parameterDescriptor, String parameterValue) {
		String parameterType = (String) parameterDescriptor.getFieldValue(ARGUMENT_TYPE_DESCRIPTOR);

		if (Boolean.class.getSimpleName().toUpperCase().equals(parameterType)) {
			return Boolean.valueOf(parameterValue)
					? parameterDescriptor.getFieldValue(ARGUMENT_NAME_DESCRIPTOR).toString() : null;
		}
		else {
			return parameterValue;
		}
	}

}
