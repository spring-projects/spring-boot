/*
 * Copyright 2012-2017 the original author or authors.
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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.springframework.boot.configurationsample.fieldvalues.FieldValues;
import org.springframework.boot.junit.compiler.TestCompiler;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base class for {@link FieldValuesParser} tests.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
public abstract class AbstractFieldValuesProcessorTests {

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	protected abstract FieldValuesParser createProcessor(ProcessingEnvironment env);

	@Test
	public void getFieldValues() throws Exception {
		TestProcessor processor = new TestProcessor();
		TestCompiler compiler = new TestCompiler(this.temporaryFolder);
		compiler.getTask(FieldValues.class).call(processor);
		Map<String, Object> values = processor.getValues();
		assertThat(values.get("string")).isEqualTo("1");
		assertThat(values.get("stringNone")).isNull();
		assertThat(values.get("stringConst")).isEqualTo("c");
		assertThat(values.get("bool")).isEqualTo(true);
		assertThat(values.get("boolNone")).isEqualTo(false);
		assertThat(values.get("boolConst")).isEqualTo(true);
		assertThat(values.get("boolObject")).isEqualTo(true);
		assertThat(values.get("boolObjectNone")).isNull();
		assertThat(values.get("boolObjectConst")).isEqualTo(true);
		assertThat(values.get("integer")).isEqualTo(1);
		assertThat(values.get("integerNone")).isEqualTo(0);
		assertThat(values.get("integerConst")).isEqualTo(2);
		assertThat(values.get("integerObject")).isEqualTo(3);
		assertThat(values.get("integerObjectNone")).isNull();
		assertThat(values.get("integerObjectConst")).isEqualTo(4);
		assertThat(values.get("charset")).isEqualTo("US-ASCII");
		assertThat(values.get("charsetConst")).isEqualTo("UTF-8");
		assertThat(values.get("mimeType")).isEqualTo("text/html");
		assertThat(values.get("mimeTypeConst")).isEqualTo("text/plain");
		assertThat(values.get("object")).isEqualTo(123);
		assertThat(values.get("objectNone")).isNull();
		assertThat(values.get("objectConst")).isEqualTo("c");
		assertThat(values.get("objectInstance")).isNull();
		assertThat(values.get("stringArray")).isEqualTo(new Object[] { "FOO", "BAR" });
		assertThat(values.get("stringArrayNone")).isNull();
		assertThat(values.get("stringEmptyArray")).isEqualTo(new Object[0]);
		assertThat(values.get("stringArrayConst")).isEqualTo(new Object[] { "OK", "KO" });
		assertThat(values.get("stringArrayConstElements"))
				.isEqualTo(new Object[] { "c" });
		assertThat(values.get("integerArray")).isEqualTo(new Object[] { 42, 24 });
		assertThat(values.get("unknownArray")).isNull();
	}

	@SupportedAnnotationTypes({
			"org.springframework.boot.configurationsample.ConfigurationProperties" })
	@SupportedSourceVersion(SourceVersion.RELEASE_6)
	private class TestProcessor extends AbstractProcessor {

		private FieldValuesParser processor;

		private Map<String, Object> values = new HashMap<String, Object>();

		@Override
		public synchronized void init(ProcessingEnvironment env) {
			this.processor = createProcessor(env);
		}

		@Override
		public boolean process(Set<? extends TypeElement> annotations,
				RoundEnvironment roundEnv) {
			for (TypeElement annotation : annotations) {
				for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
					if (element instanceof TypeElement) {
						try {
							this.values.putAll(
									this.processor.getFieldValues((TypeElement) element));
						}
						catch (Exception ex) {
							throw new IllegalStateException(ex);
						}
					}
				}
			}
			return false;
		}

		public Map<String, Object> getValues() {
			return this.values;
		}

	}

}
