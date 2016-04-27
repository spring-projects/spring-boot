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

package org.springframework.boot.test;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.test.context.ContextLoader;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.TestContextBootstrapper;
import org.springframework.test.context.support.DefaultTestContextBootstrapper;
import org.springframework.test.context.web.WebDelegatingSmartContextLoader;
import org.springframework.test.context.web.WebMergedContextConfiguration;

/**
 * {@link TestContextBootstrapper} for Spring Boot web integration tests.
 *
 * @author Phillip Webb
 * @since 1.2.1
 * @deprecated as of 1.4 along with {@link WebIntegrationTest}
 */
@Deprecated
class WebAppIntegrationTestContextBootstrapper extends DefaultTestContextBootstrapper {

	@Override
	protected Class<? extends ContextLoader> getDefaultContextLoaderClass(
			Class<?> testClass) {
		if (AnnotationUtils.findAnnotation(testClass, WebIntegrationTest.class) != null) {
			return WebDelegatingSmartContextLoader.class;
		}
		return super.getDefaultContextLoaderClass(testClass);
	}

	@Override
	protected MergedContextConfiguration processMergedContextConfiguration(
			MergedContextConfiguration mergedConfig) {
		WebIntegrationTest annotation = AnnotationUtils
				.findAnnotation(mergedConfig.getTestClass(), WebIntegrationTest.class);
		if (annotation != null) {
			mergedConfig = new WebMergedContextConfiguration(mergedConfig, null);
			MergedContextConfigurationProperties properties = new MergedContextConfigurationProperties(
					mergedConfig);
			if (annotation.randomPort()) {
				properties.add(annotation.value(), "server.port:0");
			}
			else {
				properties.add(annotation.value());
			}
		}
		return mergedConfig;
	}

}
