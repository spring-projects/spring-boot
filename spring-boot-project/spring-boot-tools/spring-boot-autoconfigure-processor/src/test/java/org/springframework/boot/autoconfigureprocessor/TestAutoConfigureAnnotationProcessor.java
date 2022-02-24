/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.autoconfigureprocessor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.annotation.processing.SupportedAnnotationTypes;

/**
 * Version of {@link AutoConfigureAnnotationProcessor} used for testing.
 *
 * @author Madhura Bhave
 */
@SupportedAnnotationTypes({ "org.springframework.boot.autoconfigureprocessor.TestConditionalOnClass",
		"org.springframework.boot.autoconfigureprocessor.TestConditionalOnBean",
		"org.springframework.boot.autoconfigureprocessor.TestConditionalOnSingleCandidate",
		"org.springframework.boot.autoconfigureprocessor.TestConditionalOnWebApplication",
		"org.springframework.boot.autoconfigureprocessor.TestAutoConfigureBefore",
		"org.springframework.boot.autoconfigureprocessor.TestAutoConfigureAfter",
		"org.springframework.boot.autoconfigureprocessor.TestAutoConfigureOrder",
		"org.springframework.boot.autoconfigureprocessor.TestAutoConfiguration" })
public class TestAutoConfigureAnnotationProcessor extends AutoConfigureAnnotationProcessor {

	private final File outputLocation;

	public TestAutoConfigureAnnotationProcessor(File outputLocation) {
		this.outputLocation = outputLocation;
	}

	@Override
	protected List<PropertyGenerator> getPropertyGenerators() {
		List<PropertyGenerator> generators = new ArrayList<>();
		String annotationPackage = "org.springframework.boot.autoconfigureprocessor";
		generators.add(PropertyGenerator.of(annotationPackage, "ConditionalOnClass")
				.withAnnotation("TestConditionalOnClass", new OnClassConditionValueExtractor()));
		generators.add(PropertyGenerator.of(annotationPackage, "ConditionalOnBean")
				.withAnnotation("TestConditionalOnBean", new OnBeanConditionValueExtractor()));
		generators.add(PropertyGenerator.of(annotationPackage, "ConditionalOnSingleCandidate")
				.withAnnotation("TestConditionalOnSingleCandidate", new OnBeanConditionValueExtractor()));
		generators.add(PropertyGenerator.of(annotationPackage, "ConditionalOnWebApplication")
				.withAnnotation("TestConditionalOnWebApplication", ValueExtractor.allFrom("type")));
		generators.add(PropertyGenerator.of(annotationPackage, "AutoConfigureBefore", true)
				.withAnnotation("TestAutoConfigureBefore", ValueExtractor.allFrom("value", "name"))
				.withAnnotation("TestAutoConfiguration", ValueExtractor.allFrom("before", "beforeName")));
		generators.add(PropertyGenerator.of(annotationPackage, "AutoConfigureAfter", true)
				.withAnnotation("TestAutoConfigureAfter", ValueExtractor.allFrom("value", "name"))
				.withAnnotation("TestAutoConfiguration", ValueExtractor.allFrom("after", "afterName")));
		generators.add(PropertyGenerator.of(annotationPackage, "AutoConfigureOrder")
				.withAnnotation("TestAutoConfigureOrder", ValueExtractor.allFrom("value")));
		return generators;
	}

	public Properties getWrittenProperties() throws IOException {
		File file = getWrittenFile();
		if (!file.exists()) {
			return null;
		}
		try (FileInputStream inputStream = new FileInputStream(file)) {
			Properties properties = new Properties();
			properties.load(inputStream);
			return properties;
		}
	}

	public File getWrittenFile() {
		return new File(this.outputLocation, PROPERTIES_PATH);
	}

}
