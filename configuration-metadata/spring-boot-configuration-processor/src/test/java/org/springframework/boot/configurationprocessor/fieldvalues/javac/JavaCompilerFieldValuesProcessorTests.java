/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationprocessor.fieldvalues.javac;

import javax.annotation.processing.ProcessingEnvironment;

import org.opentest4j.TestAbortedException;

import org.springframework.boot.configurationprocessor.fieldvalues.AbstractFieldValuesProcessorTests;
import org.springframework.boot.configurationprocessor.fieldvalues.FieldValuesParser;

/**
 * Tests for {@link JavaCompilerFieldValuesParser}.
 *
 * @author Phillip Webb
 */
class JavaCompilerFieldValuesProcessorTests extends AbstractFieldValuesProcessorTests {

	@Override
	protected FieldValuesParser createProcessor(ProcessingEnvironment env) {
		try {
			return new JavaCompilerFieldValuesParser(env);
		}
		catch (Throwable ex) {
			throw new TestAbortedException();
		}
	}

}
