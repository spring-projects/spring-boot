/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.configurationprocessor.fieldvalues.javac;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

import org.springframework.boot.configurationprocessor.fieldvalues.FieldValuesParser;

/**
 * {@link FieldValuesParser} implementation for the standard Java compiler.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 1.2.0
 */
public class JavaCompilerFieldValuesParser implements FieldValuesParser {

	private final Trees trees;

	/**
     * Constructs a new JavaCompilerFieldValuesParser object with the specified ProcessingEnvironment.
     * 
     * @param env the ProcessingEnvironment used for parsing field values
     * @throws Exception if an error occurs during initialization
     */
    public JavaCompilerFieldValuesParser(ProcessingEnvironment env) throws Exception {
		this.trees = Trees.instance(env);
	}

	/**
     * Retrieves the field values of a given TypeElement.
     * 
     * @param element the TypeElement for which to retrieve the field values
     * @return a Map containing the field names as keys and their corresponding values as values
     * @throws Exception if an error occurs while retrieving the field values
     */
    @Override
	public Map<String, Object> getFieldValues(TypeElement element) throws Exception {
		Tree tree = this.trees.getTree(element);
		if (tree != null) {
			FieldCollector fieldCollector = new FieldCollector();
			tree.accept(fieldCollector);
			return fieldCollector.getFieldValues();
		}
		return Collections.emptyMap();
	}

	/**
	 * {@link TreeVisitor} to collect fields.
	 */
	private static final class FieldCollector implements TreeVisitor {

		private static final Map<String, Class<?>> WRAPPER_TYPES;

		static {
			Map<String, Class<?>> types = new HashMap<>();
			types.put("boolean", Boolean.class);
			types.put(Boolean.class.getName(), Boolean.class);
			types.put("byte", Byte.class);
			types.put(Byte.class.getName(), Byte.class);
			types.put("short", Short.class);
			types.put(Short.class.getName(), Short.class);
			types.put("int", Integer.class);
			types.put(Integer.class.getName(), Integer.class);
			types.put("long", Long.class);
			types.put(Long.class.getName(), Long.class);
			WRAPPER_TYPES = Collections.unmodifiableMap(types);
		}

		private static final Map<Class<?>, Object> DEFAULT_TYPE_VALUES;

		static {
			Map<Class<?>, Object> values = new HashMap<>();
			values.put(Boolean.class, false);
			values.put(Byte.class, (byte) 0);
			values.put(Short.class, (short) 0);
			values.put(Integer.class, 0);
			values.put(Long.class, (long) 0);
			DEFAULT_TYPE_VALUES = Collections.unmodifiableMap(values);
		}

		private static final Map<String, Object> WELL_KNOWN_STATIC_FINALS;

		static {
			Map<String, Object> values = new HashMap<>();
			values.put("Boolean.TRUE", true);
			values.put("Boolean.FALSE", false);
			values.put("StandardCharsets.ISO_8859_1", "ISO-8859-1");
			values.put("StandardCharsets.UTF_8", "UTF-8");
			values.put("StandardCharsets.UTF_16", "UTF-16");
			values.put("StandardCharsets.US_ASCII", "US-ASCII");
			values.put("Duration.ZERO", 0);
			values.put("Period.ZERO", 0);
			WELL_KNOWN_STATIC_FINALS = Collections.unmodifiableMap(values);
		}

		private static final String DURATION_OF = "Duration.of";

		private static final Map<String, String> DURATION_SUFFIX;

		static {
			Map<String, String> values = new HashMap<>();
			values.put("Nanos", "ns");
			values.put("Millis", "ms");
			values.put("Seconds", "s");
			values.put("Minutes", "m");
			values.put("Hours", "h");
			values.put("Days", "d");
			DURATION_SUFFIX = Collections.unmodifiableMap(values);
		}

		private static final String PERIOD_OF = "Period.of";

		private static final Map<String, String> PERIOD_SUFFIX;

		static {
			Map<String, String> values = new HashMap<>();
			values.put("Days", "d");
			values.put("Weeks", "w");
			values.put("Months", "m");
			values.put("Years", "y");
			PERIOD_SUFFIX = Collections.unmodifiableMap(values);
		}

		private static final String DATA_SIZE_OF = "DataSize.of";

		private static final Map<String, String> DATA_SIZE_SUFFIX;

		static {
			Map<String, String> values = new HashMap<>();
			values.put("Bytes", "B");
			values.put("Kilobytes", "KB");
			values.put("Megabytes", "MB");
			values.put("Gigabytes", "GB");
			values.put("Terabytes", "TB");
			DATA_SIZE_SUFFIX = Collections.unmodifiableMap(values);
		}

		private final Map<String, Object> fieldValues = new HashMap<>();

		private final Map<String, Object> staticFinals = new HashMap<>();

		/**
         * Visits a variable and collects its information.
         * 
         * @param variable the variable tree to visit
         * @throws Exception if an error occurs during the visit
         */
        @Override
		public void visitVariable(VariableTree variable) throws Exception {
			Set<Modifier> flags = variable.getModifierFlags();
			if (flags.contains(Modifier.STATIC) && flags.contains(Modifier.FINAL)) {
				this.staticFinals.put(variable.getName(), getValue(variable));
			}
			if (!flags.contains(Modifier.FINAL)) {
				this.fieldValues.put(variable.getName(), getValue(variable));
			}
		}

		/**
         * Retrieves the value of a given variable.
         * 
         * @param variable the VariableTree object representing the variable
         * @return the value of the variable
         * @throws Exception if an error occurs while retrieving the value
         */
        private Object getValue(VariableTree variable) throws Exception {
			ExpressionTree initializer = variable.getInitializer();
			Class<?> wrapperType = WRAPPER_TYPES.get(variable.getType());
			Object defaultValue = DEFAULT_TYPE_VALUES.get(wrapperType);
			if (initializer != null) {
				return getValue(initializer, defaultValue);
			}
			return defaultValue;
		}

		/**
         * Retrieves the value of an expression tree.
         * 
         * @param expression the expression tree to evaluate
         * @param defaultValue the default value to return if the expression cannot be resolved
         * @return the value of the expression tree, or the default value if it cannot be resolved
         * @throws Exception if an error occurs during evaluation
         */
        private Object getValue(ExpressionTree expression, Object defaultValue) throws Exception {
			Object literalValue = expression.getLiteralValue();
			if (literalValue != null) {
				return literalValue;
			}
			Object factoryValue = expression.getFactoryValue();
			if (factoryValue != null) {
				return getFactoryValue(expression, factoryValue);
			}
			List<? extends ExpressionTree> arrayValues = expression.getArrayExpression();
			if (arrayValues != null) {
				Object[] result = new Object[arrayValues.size()];
				for (int i = 0; i < arrayValues.size(); i++) {
					Object value = getValue(arrayValues.get(i), null);
					if (value == null) { // One of the elements could not be resolved
						return defaultValue;
					}
					result[i] = value;
				}
				return result;
			}
			if (expression.getKind().equals("IDENTIFIER")) {
				return this.staticFinals.get(expression.toString());
			}
			if (expression.getKind().equals("MEMBER_SELECT")) {
				return WELL_KNOWN_STATIC_FINALS.get(expression.toString());
			}
			return defaultValue;
		}

		/**
         * Retrieves the value from the factory based on the given expression tree and factory value.
         * 
         * @param expression the expression tree representing the value to be retrieved from the factory
         * @param factoryValue the initial value from the factory
         * @return the retrieved value from the factory based on the expression tree, or the initial factory value if no matching value is found
         */
        private Object getFactoryValue(ExpressionTree expression, Object factoryValue) {
			Object durationValue = getFactoryValue(expression, factoryValue, DURATION_OF, DURATION_SUFFIX);
			if (durationValue != null) {
				return durationValue;
			}
			Object dataSizeValue = getFactoryValue(expression, factoryValue, DATA_SIZE_OF, DATA_SIZE_SUFFIX);
			if (dataSizeValue != null) {
				return dataSizeValue;
			}
			Object periodValue = getFactoryValue(expression, factoryValue, PERIOD_OF, PERIOD_SUFFIX);
			if (periodValue != null) {
				return periodValue;
			}
			return factoryValue;
		}

		/**
         * Retrieves the factory value based on the given expression, factory value, prefix, and suffix mapping.
         * 
         * @param expression the expression tree representing the instance
         * @param factoryValue the value of the factory
         * @param prefix the prefix used to identify the type in the instance string
         * @param suffixMapping the mapping of types to their corresponding suffixes
         * @return the factory value with the appropriate suffix, or null if no suffix is found
         */
        private Object getFactoryValue(ExpressionTree expression, Object factoryValue, String prefix,
				Map<String, String> suffixMapping) {
			Object instance = expression.getInstance();
			if (instance != null && instance.toString().startsWith(prefix)) {
				String type = instance.toString();
				type = type.substring(prefix.length(), type.indexOf('('));
				String suffix = suffixMapping.get(type);
				return (suffix != null) ? factoryValue + suffix : null;
			}
			return null;
		}

		/**
         * Returns the field values stored in a map.
         *
         * @return a map containing the field values
         */
        Map<String, Object> getFieldValues() {
			return this.fieldValues;
		}

	}

}
