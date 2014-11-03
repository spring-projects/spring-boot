/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.boot.configurationprocessor.TestCompiler;
import org.springframework.boot.configurationsample.fieldvalues.FieldValues;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Abstract base class for {@link FieldValuesParser} tests.
 *
 * @author Phillip Webb
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
		assertThat(values.get("string"), equalToObject("1"));
		assertThat(values.get("stringNone"), nullValue());
		assertThat(values.get("stringConst"), equalToObject("c"));
		assertThat(values.get("bool"), equalToObject(true));
		assertThat(values.get("boolNone"), equalToObject(false));
		assertThat(values.get("boolConst"), equalToObject(true));
		assertThat(values.get("boolObject"), equalToObject(true));
		assertThat(values.get("boolObjectNone"), nullValue());
		assertThat(values.get("boolObjectConst"), equalToObject(true));
		assertThat(values.get("integer"), equalToObject(1));
		assertThat(values.get("integerNone"), equalToObject(0));
		assertThat(values.get("integerConst"), equalToObject(2));
		assertThat(values.get("integerObject"), equalToObject(3));
		assertThat(values.get("integerObjectNone"), nullValue());
		assertThat(values.get("integerObjectConst"), equalToObject(4));
		assertThat(values.get("object"), equalToObject(123));
		assertThat(values.get("objectNone"), nullValue());
		assertThat(values.get("objectConst"), equalToObject("c"));
		assertThat(values.get("objectInstance"), nullValue());
	}

	private Matcher<Object> equalToObject(Object object) {
		return equalTo(object);
	}

	@SupportedAnnotationTypes({ "org.springframework.boot.configurationsample.ConfigurationProperties" })
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
							this.values.putAll(this.processor
									.getFieldValues((TypeElement) element));
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
