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

package org.springframework.boot.configurationprocessor.fieldvalues;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.configurationsample.fieldvalues.FieldValues;
import org.springframework.boot.testsupport.compiler.TestCompiler;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base class for {@link FieldValuesParser} tests.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Chris Bono
 */
public abstract class AbstractFieldValuesProcessorTests {

	@TempDir
	File tempDir;

	protected abstract FieldValuesParser createProcessor(ProcessingEnvironment env);

	@Test
	void getFieldValues() throws Exception {
		TestProcessor processor = new TestProcessor();
		TestCompiler compiler = new TestCompiler(this.tempDir);
		compiler.getTask(FieldValues.class).call(processor);

		Map<String, ValueWrapper> values = processor.getValues();

		assertThatValuesHasFieldWithNameValueAndExpression(values,"string", "1", "\"1\"");
		assertThatValuesHasNoFieldWithName(values, "stringNone");
		assertThatValuesHasFieldWithNameValueAndExpression(values,"stringNull", null, "null");
		assertThatValuesHasFieldWithNameValueAndExpression(values, "stringConst","c", "\"c\"");
		assertThatValuesHasFieldWithNameValueAndExpression(values, "stringConstNull", null, "null");
		assertThatValuesHasFieldWithNameExpressionAndUndeterminedValue(values, "stringFinal", "STRING_FINAL");
		assertThatValuesHasFieldWithNameValueAndExpression(values, "bool", true, "true");
		assertThatValuesHasFieldWithNameValueAndExpression(values, "boolNone", false, null);
		assertThatValuesHasFieldWithNameValueAndExpression(values, "boolConst", true, "true");
		assertThatValuesHasFieldWithNameValueAndExpression(values, "boolObject", true, "Boolean.TRUE");
		assertThatValuesHasNoFieldWithName(values, "boolObjectNone");
		assertThatValuesHasFieldWithNameValueAndExpression(values, "boolObjectConst",true, "true");
		assertThatValuesHasFieldWithNameValueAndExpression(values, "integer", 1, "1");
		assertThatValuesHasFieldWithNameValueAndExpression(values, "integerNone", 0, null);
		assertThatValuesHasFieldWithNameValueAndExpression(values, "integerConst", 2, "2");
		assertThatValuesHasFieldWithNameValueAndExpression(values, "integerObject", 3, "3");
		assertThatValuesHasNoFieldWithName(values, "integerObjectNone");
		assertThatValuesHasFieldWithNameValueAndExpression(values, "integerObjectConst", 4, "4");
		assertThatValuesHasFieldWithNameValueAndExpression(values, "charset", "US-ASCII", "StandardCharsets.US_ASCII");
		assertThatValuesHasFieldWithNameValueAndExpression(values, "charsetConst", "UTF-8", "StandardCharsets.UTF_8");
		assertThatValuesHasFieldWithNameValueAndExpression(values, "mimeType", "text/html", "MimeType.valueOf(\"text/html\")");
		assertThatValuesHasFieldWithNameValueAndExpression(values, "mimeTypeConst", "text/plain", "MimeType.valueOf(\"text/plain\")");
		assertThatValuesHasFieldWithNameValueAndExpression(values, "object", 123, "123");
		assertThatValuesHasNoFieldWithName(values, "objectNone");
		assertThatValuesHasFieldWithNameValueAndExpression(values, "objectConst", "c", "\"c\"");
		assertThatValuesHasFieldWithNameExpressionAndUndeterminedValue(values, "objectInstance", "new StringBuffer()");
		assertThatValuesHasFieldWithNameValueAndExpression(values, "stringArray", new String[] {"FOO", "BAR"}, "new String[]{\"FOO\", \"BAR\"}");
		assertThatValuesHasNoFieldWithName(values, "stringArrayNone");
		assertThatValuesHasFieldWithNameValueAndExpression(values, "stringEmptyArray", new String[0], "new String[0]");
		assertThatValuesHasFieldWithNameValueAndExpression(values, "stringArrayConst", new String[] { "OK", "KO" }, "new String[]{\"OK\", \"KO\"}");
		assertThatValuesHasFieldWithNameValueAndExpression(values, "stringArrayConstElements", new String[] { "c" }, "new String[]{STRING_CONST}");
		assertThatValuesHasFieldWithNameValueAndExpression(values, "integerArray", new Integer[] { 42, 24 }, "new Integer[]{42, 24}");
		assertThatValuesHasFieldWithNameExpressionAndUndeterminedValue(values, "objectArrayBadEntry", "new StringBuffer()");
		assertThatValuesHasFieldWithNameExpressionAndUndeterminedValue(values, "unknownArray", "new UnknownElementType()");
		assertThatValuesHasNoFieldWithName(values, "durationNone");
		assertThatValuesHasFieldWithNameValueAndExpression(values, "durationNanos", "5ns", "Duration.ofNanos(5)");
		assertThatValuesHasFieldWithNameValueAndExpression(values, "durationMillis", "10ms", "Duration.ofMillis(10)");
		assertThatValuesHasFieldWithNameValueAndExpression(values, "durationSeconds", "20s", "Duration.ofSeconds(20)");
		assertThatValuesHasFieldWithNameValueAndExpression(values, "durationMinutes", "30m", "Duration.ofMinutes(30)");
		assertThatValuesHasFieldWithNameValueAndExpression(values, "durationHours", "40h", "Duration.ofHours(40)");
		assertThatValuesHasFieldWithNameValueAndExpression(values, "durationDays", "50d", "Duration.ofDays(50)");
		assertThatValuesHasFieldWithNameValueAndExpression(values, "durationZero", 0, "Duration.ZERO");
		assertThatValuesHasNoFieldWithName(values, "dataSizeNone");
		assertThatValuesHasFieldWithNameValueAndExpression(values, "dataSizeBytes", "5B", "DataSize.ofBytes(5)");
		assertThatValuesHasFieldWithNameValueAndExpression(values, "dataSizeKilobytes", "10KB", "DataSize.ofKilobytes(10)");
		assertThatValuesHasFieldWithNameValueAndExpression(values, "dataSizeMegabytes", "20MB", "DataSize.ofMegabytes(20)");
		assertThatValuesHasFieldWithNameValueAndExpression(values, "dataSizeGigabytes", "30GB", "DataSize.ofGigabytes(30)");
		assertThatValuesHasFieldWithNameValueAndExpression(values, "dataSizeTerabytes", "40TB", "DataSize.ofTerabytes(40)");
		assertThatValuesHasNoFieldWithName(values, "periodNone");
		assertThatValuesHasFieldWithNameValueAndExpression(values, "periodDays", "3d", "Period.ofDays(3)");
		assertThatValuesHasFieldWithNameValueAndExpression(values, "periodWeeks", "2w", "Period.ofWeeks(2)");
		assertThatValuesHasFieldWithNameValueAndExpression(values, "periodMonths", "10m", "Period.ofMonths(10)");
		assertThatValuesHasFieldWithNameValueAndExpression(values, "periodYears", "15y", "Period.ofYears(15)");
		assertThatValuesHasFieldWithNameValueAndExpression(values, "periodZero", 0, "Period.ZERO");
	}

	private void assertThatValuesHasFieldWithNameValueAndExpression(Map<String, ValueWrapper> values, String fieldName, Object expectedValue, String expectedExpression) {
		ValueWrapper valueWrapper = values.get(fieldName);
		assertThat(valueWrapper).isNotNull();
		assertThat(valueWrapper.valueDetermined()).isTrue();
		assertThat(valueWrapper.valueInitializerExpression()).isEqualTo(expectedExpression);
		if (Arrays.isArray(expectedValue)) {
			Object[] expectedValues = Arrays.asObjectArray(expectedValue);
			assertThat(valueWrapper.value()).asInstanceOf(InstanceOfAssertFactories.ARRAY).containsExactly(expectedValues);
		} else {
			assertThat(valueWrapper.value()).isEqualTo(expectedValue);
		}
	}

	private void assertThatValuesHasFieldWithNameExpressionAndUndeterminedValue(Map<String, ValueWrapper> values, String fieldName, String expectedExpression) {
		ValueWrapper valueWrapper = values.get(fieldName);
		assertThat(valueWrapper).isNotNull();
		assertThat(valueWrapper.valueDetermined()).isFalse();
		assertThat(valueWrapper.value()).isNull();
		assertThat(valueWrapper.valueInitializerExpression()).isEqualTo(expectedExpression);
	}

	private void assertThatValuesHasNoFieldWithName(Map<String, ValueWrapper> values, String fieldName) {
		ValueWrapper valueWrapper = values.get(fieldName);
		assertThat(valueWrapper).isNull();
	}
	
	@SupportedAnnotationTypes({ "org.springframework.boot.configurationsample.ConfigurationProperties" })
	@SupportedSourceVersion(SourceVersion.RELEASE_6)
	private class TestProcessor extends AbstractProcessor {

		private FieldValuesParser processor;

		private Map<String, ValueWrapper> values = new HashMap<>();

		@Override
		public synchronized void init(ProcessingEnvironment env) {
			this.processor = createProcessor(env);
		}

		@Override
		public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
			for (TypeElement annotation : annotations) {
				for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
					if (element instanceof TypeElement) {
						try {
							this.values.putAll(this.processor.getFieldValues((TypeElement) element));
						}
						catch (Exception ex) {
							throw new IllegalStateException(ex);
						}
					}
				}
			}
			return false;
		}

		Map<String, ValueWrapper> getValues() {
			return this.values;
		}

	}

}
