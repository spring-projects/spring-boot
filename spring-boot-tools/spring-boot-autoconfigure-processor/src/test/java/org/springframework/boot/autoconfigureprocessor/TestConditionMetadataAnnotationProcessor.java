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

package org.springframework.boot.autoconfigureprocessor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import javax.annotation.processing.SupportedAnnotationTypes;

/**
 * Version of {@link AutoConfigureAnnotationProcessor} used for testing.
 *
 * @author Madhura Bhave
 */
@SupportedAnnotationTypes({
		"org.springframework.boot.autoconfigureprocessor.TestConfiguration",
		"org.springframework.boot.autoconfigureprocessor.TestConditionalOnClass",
		"org.springframework.boot.autoconfigureprocessor.TestAutoConfigureBefore",
		"org.springframework.boot.autoconfigureprocessor.TestAutoConfigureAfter",
		"org.springframework.boot.autoconfigureprocessor.TestAutoConfigureOrder" })
public class TestConditionMetadataAnnotationProcessor
		extends AutoConfigureAnnotationProcessor {

	private final File outputLocation;

	public TestConditionMetadataAnnotationProcessor(File outputLocation) {
		this.outputLocation = outputLocation;
	}

	@Override
	protected void addAnnotations(Map<String, String> annotations) {
		put(annotations, "Configuration", TestConfiguration.class);
		put(annotations, "ConditionalOnClass", TestConditionalOnClass.class);
		put(annotations, "AutoConfigureBefore", TestAutoConfigureBefore.class);
		put(annotations, "AutoConfigureAfter", TestAutoConfigureAfter.class);
		put(annotations, "AutoConfigureOrder", TestAutoConfigureOrder.class);
	}

	private void put(Map<String, String> annotations, String key, Class<?> value) {
		annotations.put(key, value.getName());
	}

	public Properties getWrittenProperties() throws IOException {
		File file = new File(this.outputLocation, PROPERTIES_PATH);
		if (!file.exists()) {
			return null;
		}
		FileInputStream inputStream = new FileInputStream(file);
		try {
			Properties properties = new Properties();
			properties.load(inputStream);
			return properties;
		}
		finally {
			inputStream.close();
		}
	}

}
