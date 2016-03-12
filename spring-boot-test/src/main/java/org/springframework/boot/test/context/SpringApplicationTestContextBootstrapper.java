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

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestContextBootstrapper;
import org.springframework.test.context.web.ServletTestExecutionListener;
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

	@Override
	public TestContext buildTestContext() {
		TestContext context = super.buildTestContext();
		if (isWebApplicationTest()) {
			context.setAttribute(ServletTestExecutionListener.ACTIVATE_LISTENER, true);
		}
		return context;
	}

	@Override
	protected MergedContextConfiguration processMergedContextConfiguration(
			MergedContextConfiguration mergedConfig) {
		mergedConfig = super.processMergedContextConfiguration(mergedConfig);
		if (!(mergedConfig instanceof WebMergedContextConfiguration)
				&& isWebApplicationTest()) {
			mergedConfig = new WebMergedContextConfiguration(mergedConfig, "");
		}
		return mergedConfig;
	}

	private boolean isWebApplicationTest() {
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
		propertySourceProperties.addAll(Arrays.asList(annotation.value()));
	}

}
