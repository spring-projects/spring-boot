/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.web.server.test.client;

import java.util.List;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.TestContextAnnotationUtils;
import org.springframework.util.ClassUtils;

/**
 * {@link ContextCustomizerFactory} for {@link TestRestTemplate}.
 *
 * @author Andy Wilkinson
 * @see TestRestTemplateContextCustomizer
 */
class TestRestTemplateContextCustomizerFactory implements ContextCustomizerFactory {

	private static final boolean REST_TEMPLATE_BUILDER_PRESENT = ClassUtils.isPresent(
			"org.springframework.boot.restclient.RestTemplateBuilder",
			TestRestTemplateContextCustomizerFactory.class.getClassLoader());

	@Override
	public ContextCustomizer createContextCustomizer(Class<?> testClass,
			List<ContextConfigurationAttributes> configAttributes) {
		if (!REST_TEMPLATE_BUILDER_PRESENT) {
			return null;
		}
		SpringBootTest springBootTest = TestContextAnnotationUtils.findMergedAnnotation(testClass,
				SpringBootTest.class);
		return (springBootTest != null) ? new TestRestTemplateContextCustomizer() : null;
	}

}
