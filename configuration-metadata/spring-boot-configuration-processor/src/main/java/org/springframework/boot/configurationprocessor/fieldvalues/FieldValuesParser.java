/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationprocessor.fieldvalues;

import java.util.Collections;
import java.util.Map;

import javax.lang.model.element.TypeElement;

import org.springframework.boot.configurationprocessor.fieldvalues.javac.JavaCompilerFieldValuesParser;

/**
 * Parser which can be used to obtain the field values from an {@link TypeElement}.
 *
 * @author Phillip Webb
 * @since 1.1.2
 * @see JavaCompilerFieldValuesParser
 */
public interface FieldValuesParser {

	/**
	 * Implementation of {@link FieldValuesParser} that always returns an empty result.
	 */
	FieldValuesParser NONE = new FieldValuesParser() {

		@Override
		public Map<String, Object> getFieldValues(TypeElement element) {
			return Collections.emptyMap();
		}

	};

	/**
	 * Return the field values for the given element.
	 * @param element the element to inspect
	 * @return a map of field names to values.
	 * @throws Exception if the values cannot be extracted
	 */
	Map<String, Object> getFieldValues(TypeElement element) throws Exception;

	/**
	 * Return whether the given element has a source tree available. Elements loaded from
	 * {@code .class} files during incremental compilation will not have a source tree,
	 * meaning javadoc comments are unavailable.
	 * @param element the element to check
	 * @return {@code true} if the element has source, {@code false} if backed by
	 * {@code .class}
	 */
	default boolean hasSourceTree(TypeElement element) {
		return true;
	}

}
