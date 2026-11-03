/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationprocessor.fieldvalues.javac;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Period;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

import org.springframework.boot.configurationprocessor.fieldvalues.FieldValuesParser;
import org.springframework.boot.configurationprocessor.fieldvalues.javac.ExpressionTree.Member;
import org.springframework.boot.configurationprocessor.support.ConventionUtils;

/**
 * {@link FieldValuesParser} implementation for the standard Java compiler.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 1.2.0
 */
public class JavaCompilerFieldValuesParser implements FieldValuesParser {

	private final Trees trees;

	public JavaCompilerFieldValuesParser(ProcessingEnvironment env) throws Exception {
		this.trees = Trees.instance(env);
	}

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

	@Override
	public boolean hasSourceTree(TypeElement element) {
		try {
			return this.trees.getTree(element) != null;
		}
		catch (Exception ex) {
			return true;
		}
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

		private static final KnownType BOOLEAN = KnownType.of(Boolean.class);

		private static final KnownType STANDARD_CHARSETS = KnownType.of(StandardCharsets.class);

		private static final KnownType DURATION = KnownType.of(Duration.class);

		private static final KnownType PERIOD = KnownType.of(Period.class);

		private static final KnownType DATA_SIZE = new KnownType("org.springframework.util.unit", "DataSize");

		private static final Map<KnownType, Map<String, Object>> WELL_KNOWN_STATIC_FINALS = Map.of(BOOLEAN,
				Map.of("TRUE", true, "FALSE", false), STANDARD_CHARSETS,
				Map.of("ISO_8859_1", "ISO-8859-1", "UTF_8", "UTF-8", "UTF_16", "UTF-16", "US_ASCII", "US-ASCII"),
				DURATION, Map.of("ZERO", 0), PERIOD, Map.of("ZERO", 0));

		private static final Map<KnownType, Map<String, String>> FACTORY_SUFFIXES = Map.of(DURATION,
				Map.of("ofNanos", "ns", "ofMillis", "ms", "ofSeconds", "s", "ofMinutes", "m", "ofHours", "h", "ofDays",
						"d"),
				PERIOD, Map.of("ofDays", "d", "ofWeeks", "w", "ofMonths", "m", "ofYears", "y"), DATA_SIZE,
				Map.of("ofBytes", "B", "ofKilobytes", "KB", "ofMegabytes", "MB", "ofGigabytes", "GB", "ofTerabytes",
						"TB"));

		private final Map<String, Object> fieldValues = new HashMap<>();

		private final Map<String, Object> staticFinals = new HashMap<>();

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

		private Object getValue(VariableTree variable) throws Exception {
			ExpressionTree initializer = variable.getInitializer();
			Class<?> wrapperType = WRAPPER_TYPES.get(variable.getType());
			Object defaultValue = DEFAULT_TYPE_VALUES.get(wrapperType);
			if (initializer != null) {
				return getValue(variable.getType(), initializer, defaultValue);
			}
			return defaultValue;
		}

		private Object getValue(String variableType, ExpressionTree expression, Object defaultValue) throws Exception {
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
					Object value = getValue(variableType, arrayValues.get(i), null);
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
				Object value = getWellKnownStaticFinal(expression.toString());
				if (value != null) {
					return value;
				}
				Member selectedMember = expression.getSelectedMember();
				// Type matching the expression, assuming an enum
				if (selectedMember != null && selectedMember.expression().equals(variableType)) {
					return ConventionUtils.toDashedCase(selectedMember.identifier().toLowerCase(Locale.ENGLISH));
				}
				return null;
			}
			return null;
		}

		private Object getWellKnownStaticFinal(String expression) {
			for (Map.Entry<KnownType, Map<String, Object>> entry : WELL_KNOWN_STATIC_FINALS.entrySet()) {
				String member = entry.getKey().getMember(expression);
				if (member != null && entry.getValue().containsKey(member)) {
					return entry.getValue().get(member);
				}
			}
			return null;
		}

		private Object getFactoryValue(ExpressionTree expression, Object factoryValue) {
			Object instance = expression.getInstance();
			if (instance != null) {
				for (Map.Entry<KnownType, Map<String, String>> entry : FACTORY_SUFFIXES.entrySet()) {
					String call = entry.getKey().getMember(instance.toString());
					String suffix = (call != null) ? entry.getValue().get(call.substring(0, call.indexOf('('))) : null;
					if (suffix != null) {
						return factoryValue + suffix;
					}
				}
			}
			return factoryValue;
		}

		Map<String, Object> getFieldValues() {
			return this.fieldValues;
		}

	}

	/**
	 * A well-known type that can be referenced by its simple name or by its fully
	 * qualified name.
	 */
	static final class KnownType {

		private final String simplePrefix;

		private final String qualifiedPrefix;

		KnownType(String packageName, String simpleName) {
			this.simplePrefix = simpleName + ".";
			this.qualifiedPrefix = packageName + "." + simpleName + ".";
		}

		static KnownType of(Class<?> type) {
			return new KnownType(type.getPackageName(), type.getSimpleName());
		}

		/**
		 * Return the member that the given expression selects on this type, or
		 * {@code null} if the expression does not select a member of this type.
		 * @param expression the expression to check
		 * @return the selected member or {@code null}
		 */
		String getMember(String expression) {
			if (expression.startsWith(this.qualifiedPrefix)) {
				return expression.substring(this.qualifiedPrefix.length());
			}
			if (expression.startsWith(this.simplePrefix)) {
				return expression.substring(this.simplePrefix.length());
			}
			return null;
		}

	}

}
