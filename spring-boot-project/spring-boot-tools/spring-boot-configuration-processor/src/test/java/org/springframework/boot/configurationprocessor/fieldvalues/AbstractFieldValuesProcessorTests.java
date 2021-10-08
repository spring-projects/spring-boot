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

		assertThat(values.get("string")).isEqualTo(ValueWrapper.of("1", "\"1\""));
		assertThat(values.get("stringNone")).isNull();
		assertThat(values.get("stringNull")).isEqualTo(ValueWrapper.of(null, "null"));
		assertThat(values.get("stringConst")).isEqualTo(ValueWrapper.of("c", "\"c\""));
		assertThat(values.get("stringConstNull")).isEqualTo(ValueWrapper.of(null, "null"));
		assertThat(values.get("stringFinal")).isEqualTo(ValueWrapper.unresolvable("this.STRING_FINAL"));
		assertThat(values.get("bool")).isEqualTo(ValueWrapper.of(true, "true"));
		assertThat(values.get("boolNone")).isEqualTo(ValueWrapper.of(false, null));
		assertThat(values.get("boolConst")).isEqualTo(ValueWrapper.of(true, "true"));
		assertThat(values.get("boolObject")).isEqualTo(ValueWrapper.of(true, "Boolean.TRUE"));
		assertThat(values.get("boolObjectNone")).isNull();
		assertThat(values.get("boolObjectConst")).isEqualTo(ValueWrapper.of(true, "true"));
		assertThat(values.get("integer")).isEqualTo(ValueWrapper.of(1, "1"));
		assertThat(values.get("integerNone")).isEqualTo(ValueWrapper.of(0, null));
		assertThat(values.get("integerConst")).isEqualTo(ValueWrapper.of(2, "2"));
		assertThat(values.get("integerObject")).isEqualTo(ValueWrapper.of(3, "3"));
		assertThat(values.get("integerObjectNone")).isNull();
		assertThat(values.get("integerObjectConst")).isEqualTo(ValueWrapper.of(4, "4"));
		assertThat(values.get("charset")).isEqualTo(ValueWrapper.of("US-ASCII", "StandardCharsets.US_ASCII"));
		assertThat(values.get("charsetConst")).isEqualTo(ValueWrapper.of("UTF-8", "StandardCharsets.UTF_8"));
		assertThat(values.get("mimeType")).isEqualTo(ValueWrapper.of("text/html", "MimeType.valueOf(\"text/html\")"));
		assertThat(values.get("mimeTypeConst"))
				.isEqualTo(ValueWrapper.of("text/plain", "MimeType.valueOf(\"text/plain\")"));
		assertThat(values.get("object")).isEqualTo(ValueWrapper.of(123, "123"));
		assertThat(values.get("objectNone")).isNull();
		assertThat(values.get("objectConst")).isEqualTo(ValueWrapper.of("c", "\"c\""));
		assertThat(values.get("objectInstance")).isEqualTo(ValueWrapper.unresolvable("new StringBuffer()"));
		assertThat(values.get("stringArray"))
				.isEqualTo(ValueWrapper.of(new String[] { "FOO", "BAR" }, "new String[]{\"FOO\", \"BAR\"}"));
		assertThat(values.get("stringArrayNone")).isNull();
		assertThat(values.get("stringEmptyArray")).isEqualTo(ValueWrapper.of(new String[0], "new String[0]"));
		assertThat(values.get("stringArrayConst"))
				.isEqualTo(ValueWrapper.of(new String[] { "OK", "KO" }, "new String[]{\"OK\", \"KO\"}"));
		assertThat(values.get("stringArrayConstElements"))
				.isEqualTo(ValueWrapper.of(new String[] { "c" }, "new String[]{STRING_CONST}"));
		assertThat(values.get("integerArray"))
				.isEqualTo(ValueWrapper.of(new Integer[] { 42, 24 }, "new Integer[]{42, 24}"));
		assertThat(values.get("objectArrayBadEntry")).isEqualTo(ValueWrapper.unresolvable("new StringBuffer()"));
		assertThat(values.get("unknownArray")).isEqualTo(ValueWrapper.unresolvable("new UnknownElementType()"));
		assertThat(values.get("durationNone")).isNull();
		assertThat(values.get("durationNanos")).isEqualTo(ValueWrapper.of("5ns", "Duration.ofNanos(5)"));
		assertThat(values.get("durationMillis")).isEqualTo(ValueWrapper.of("10ms", "Duration.ofMillis(10)"));
		assertThat(values.get("durationSeconds")).isEqualTo(ValueWrapper.of("20s", "Duration.ofSeconds(20)"));
		assertThat(values.get("durationMinutes")).isEqualTo(ValueWrapper.of("30m", "Duration.ofMinutes(30)"));
		assertThat(values.get("durationHours")).isEqualTo(ValueWrapper.of("40h", "Duration.ofHours(40)"));
		assertThat(values.get("durationDays")).isEqualTo(ValueWrapper.of("50d", "Duration.ofDays(50)"));
		assertThat(values.get("durationZero")).isEqualTo(ValueWrapper.of(0, "Duration.ZERO"));
		assertThat(values.get("dataSizeNone")).isNull();
		assertThat(values.get("dataSizeBytes")).isEqualTo(ValueWrapper.of("5B", "DataSize.ofBytes(5)"));
		assertThat(values.get("dataSizeKilobytes")).isEqualTo(ValueWrapper.of("10KB", "DataSize.ofKilobytes(10)"));
		assertThat(values.get("dataSizeMegabytes")).isEqualTo(ValueWrapper.of("20MB", "DataSize.ofMegabytes(20)"));
		assertThat(values.get("dataSizeGigabytes")).isEqualTo(ValueWrapper.of("30GB", "DataSize.ofGigabytes(30)"));
		assertThat(values.get("dataSizeTerabytes")).isEqualTo(ValueWrapper.of("40TB", "DataSize.ofTerabytes(40)"));
		assertThat(values.get("periodNone")).isNull();
		assertThat(values.get("periodDays")).isEqualTo(ValueWrapper.of("3d", "Period.ofDays(3)"));
		assertThat(values.get("periodWeeks")).isEqualTo(ValueWrapper.of("2w", "Period.ofWeeks(2)"));
		assertThat(values.get("periodMonths")).isEqualTo(ValueWrapper.of("10m", "Period.ofMonths(10)"));
		assertThat(values.get("periodYears")).isEqualTo(ValueWrapper.of("15y", "Period.ofYears(15)"));
		assertThat(values.get("periodZero")).isEqualTo(ValueWrapper.of(0, "Period.ZERO"));
		assertThat(values.get("emptyArrayList")).isEqualTo(ValueWrapper.of("[]", "new ArrayList<>()"));
		assertThat(values.get("emptyArrayListWithArg"))
				.isEqualTo(ValueWrapper.unresolvable("new ArrayList<>(new HashSet<>())"));
		assertThat(values.get("emptyFullyQualifiedLinkedListRedundantType"))
				.isEqualTo(ValueWrapper.of("[]", "new java.util.LinkedList<String>()"));
		assertThat(values.get("emptyHashSet")).isEqualTo(ValueWrapper.of("[]", "new HashSet<>()"));
		assertThat(values.get("emptyFullyQualifiedHashSetRedundantType"))
				.isEqualTo(ValueWrapper.of("[]", "new java.util.HashSet<String>()"));
		assertThat(values.get("emptyTreeSet")).isEqualTo(ValueWrapper.of("[]", "new TreeSet<>()"));
		assertThat(values.get("emptyHashMap")).isEqualTo(ValueWrapper.of("{}", "new HashMap<>()"));
		assertThat(values.get("emptyHashMapWithArg"))
				.isEqualTo(ValueWrapper.unresolvable("new HashMap<>(new HashMap<>())"));
		assertThat(values.get("emptyFullyQualifiedTreeMapRedundantType"))
				.isEqualTo(ValueWrapper.of("{}", "new java.util.TreeMap<String, String>()"));
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
