/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.test.context.web;

import java.util.Arrays;
import java.util.List;

import org.springframework.boot.test.context.SpringBootTestContextBootstrapper;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.TestContextBootstrapper;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.context.web.WebMergedContextConfiguration;

/**
 * {@link TestContextBootstrapper} for Spring Boot web integration tests.
 *
 * @author Phillip Webb
 */
class WebIntegrationTestContextBootstrapper extends SpringBootTestContextBootstrapper {

	@Override
	protected MergedContextConfiguration processMergedContextConfiguration(
			MergedContextConfiguration mergedConfig) {
		assertValidAnnotations(mergedConfig.getTestClass());
		mergedConfig = super.processMergedContextConfiguration(mergedConfig);
		return new WebMergedContextConfiguration(mergedConfig, null);
	}

	private void assertValidAnnotations(Class<?> testClass) {
		if (AnnotatedElementUtils.findMergedAnnotation(testClass,
				WebAppConfiguration.class) != null
				&& AnnotatedElementUtils.findMergedAnnotation(testClass,
						WebIntegrationTest.class) != null) {
			throw new IllegalStateException("@WebIntegrationTest and "
					+ "@WebAppConfiguration cannot be used together");
		}
	}

	@Override
	protected void processPropertySourceProperties(
			MergedContextConfiguration mergedConfig,
			List<String> propertySourceProperties) {
		WebIntegrationTest annotation = AnnotatedElementUtils.getMergedAnnotation(
				mergedConfig.getTestClass(), WebIntegrationTest.class);
		propertySourceProperties.addAll(Arrays.asList(annotation.value()));
		if (annotation.randomPort()) {
			propertySourceProperties.add("server.port=0");
		}
	}

}
