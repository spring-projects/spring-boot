/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.context.config;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * JUnit {@link Extension @Extension} to switch a test to use legacy processing.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
public class UseLegacyProcessing implements BeforeAllCallback, AfterAllCallback {

	private static final String PROPERTY_NAME = "spring.config.use-legacy-processing";

	private String propertyValue;

	@Override
	public void beforeAll(ExtensionContext context) throws Exception {
		this.propertyValue = System.setProperty(PROPERTY_NAME, "true");
	}

	@Override
	public void afterAll(ExtensionContext context) throws Exception {
		if (this.propertyValue != null) {
			System.setProperty(PROPERTY_NAME, this.propertyValue);
		}
		else {
			System.clearProperty(PROPERTY_NAME);
		}
	}

}
