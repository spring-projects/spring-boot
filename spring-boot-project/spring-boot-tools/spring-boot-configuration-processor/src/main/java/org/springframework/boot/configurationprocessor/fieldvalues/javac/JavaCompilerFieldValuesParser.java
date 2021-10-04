/*
 * Copyright 2012-2021 the original author or authors.
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
import org.springframework.boot.configurationprocessor.fieldvalues.ValueWrapper;

/**
 * {@link FieldValuesParser} implementation for the standard Java compiler.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Chris Bono
 * @since 1.2.0
 */
public class JavaCompilerFieldValuesParser implements FieldValuesParser {

	private final Trees trees;

	public JavaCompilerFieldValuesParser(ProcessingEnvironment env) throws Exception {
		this.trees = Trees.instance(env);
	}

	@Override
	public Map<String, ValueWrapper> getFieldValues(TypeElement element) throws Exception {
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
	private static class FieldCollector implements TreeVisitor {

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

		private final Map<String, ValueWrapper> fieldValues = new HashMap<>();

		private final Map<String, ValueWrapper> staticFinals = new HashMap<>();

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

		private ValueWrapper getValue(VariableTree variable) throws Exception {
			ExpressionTree initializer = variable.getInitializer();
			if (initializer != null) {
				return getValue(initializer);
			}
			Class<?> wrapperType = WRAPPER_TYPES.get(variable.getType());
			if (DEFAULT_TYPE_VALUES.containsKey(wrapperType)) {
				Object defaultValue = DEFAULT_TYPE_VALUES.get(wrapperType);
				return ValueWrapper.of(defaultValue, null);
			}
			return null;
		}

		private ValueWrapper getValue(ExpressionTree expression) throws Exception {
			if (expression.hasLiteralValue()) {
				Object literalValue = expression.getLiteralValue();
				return ValueWrapper.of(literalValue, expression.toString());
			}

			Object factoryValue = expression.getFactoryValue();
			if (factoryValue != null) {
				Object resolvedValue = getFactoryValue(expression, factoryValue);
				if (resolvedValue != null) {
					return ValueWrapper.of(resolvedValue, expression.toString());
				}
				return ValueWrapper.unresolvable(expression.toString());
			}

			List<? extends ExpressionTree> arrayValues = expression.getArrayExpression();
			if (arrayValues != null) {
				Object[] result = new Object[arrayValues.size()];
				for (int i = 0; i < arrayValues.size(); i++) {
					ValueWrapper valueWrapper = getValue(arrayValues.get(i));
					if (!valueWrapper.valueDetermined()) { // an element could not be
															// resolved
						return ValueWrapper.unresolvable(arrayValues.get(i).toString());
					}
					result[i] = valueWrapper.value();
				}
				return ValueWrapper.of(result, expression.toString());
			}

			if (expression.getKind().equals("IDENTIFIER")) {
				String expressionStr = expression.toString();
				if (this.staticFinals.containsKey(expressionStr)) {
					return this.staticFinals.get(expressionStr);
				}
				return ValueWrapper.unresolvable(expressionStr);
			}

			if (expression.getKind().equals("MEMBER_SELECT")) {
				String expressionStr = expression.toString();
				if (WELL_KNOWN_STATIC_FINALS.containsKey(expressionStr)) {
					Object resolvedValue = WELL_KNOWN_STATIC_FINALS.get(expressionStr);
					return ValueWrapper.of(resolvedValue, expressionStr);
				}
				return ValueWrapper.unresolvable(expressionStr);
			}

			return ValueWrapper.unresolvable(expression.toString());
		}

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

		Map<String, ValueWrapper> getFieldValues() {
			return this.fieldValues;
		}

	}

}
