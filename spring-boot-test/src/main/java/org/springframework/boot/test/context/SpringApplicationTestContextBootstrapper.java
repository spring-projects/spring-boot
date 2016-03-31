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

package org.springframework.boot.test.context;

import java.util.Arrays;
import java.util.List;

import org.springframework.boot.test.context.SpringApplicationTest.WebEnvironment;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestContextBootstrapper;
import org.springframework.test.context.web.WebMergedContextConfiguration;
import org.springframework.util.ClassUtils;

/**
 * {@link TestContextBootstrapper} for {@link SpringApplicationTest}.
 *
 * @author Phillip Webb
 */
class SpringApplicationTestContextBootstrapper extends SpringBootTestContextBootstrapper {

	private static final String[] WEB_ENVIRONMENT_CLASSES = { "javax.servlet.Servlet",
			"org.springframework.web.context.ConfigurableWebApplicationContext" };

	private static final String ACTIVATE_SERVLET_LISTENER = "org.springframework.test."
			+ "context.web.ServletTestExecutionListener.activateListener";

	@Override
	public TestContext buildTestContext() {
		TestContext context = super.buildTestContext();
		SpringApplicationTest annotation = AnnotatedElementUtils
				.getMergedAnnotation(context.getTestClass(), SpringApplicationTest.class);
		WebEnvironment webEnvironment = annotation.webEnvironment();
		if (webEnvironment == WebEnvironment.MOCK) {
			context.setAttribute(ACTIVATE_SERVLET_LISTENER, true);
		}
		else if (webEnvironment.isEmbedded()) {
			context.setAttribute(ACTIVATE_SERVLET_LISTENER, false);
		}
		return context;
	}

	@Override
	protected MergedContextConfiguration processMergedContextConfiguration(
			MergedContextConfiguration mergedConfig) {
		mergedConfig = super.processMergedContextConfiguration(mergedConfig);
		SpringApplicationTest annotation = AnnotatedElementUtils.getMergedAnnotation(
				mergedConfig.getTestClass(), SpringApplicationTest.class);
		WebEnvironment webEnvironment = annotation.webEnvironment();
		if (webEnvironment.isEmbedded() || (webEnvironment == WebEnvironment.MOCK
				&& hasWebEnvironmentClasses())) {
			mergedConfig = new WebMergedContextConfiguration(mergedConfig, "");
		}
		return mergedConfig;
	}

	private boolean hasWebEnvironmentClasses() {
		for (String className : WEB_ENVIRONMENT_CLASSES) {
			if (!ClassUtils.isPresent(className, null)) {
				return false;
			}
		}
		return true;
	}

	@Override
	protected void processPropertySourceProperties(
			MergedContextConfiguration mergedConfig,
			List<String> propertySourceProperties) {
		SpringApplicationTest annotation = AnnotatedElementUtils.getMergedAnnotation(
				mergedConfig.getTestClass(), SpringApplicationTest.class);
		propertySourceProperties.addAll(Arrays.asList(annotation.properties()));
		if (annotation.webEnvironment() == WebEnvironment.RANDOM_PORT) {
			propertySourceProperties.add("server.port=0");
		}
	}

}
