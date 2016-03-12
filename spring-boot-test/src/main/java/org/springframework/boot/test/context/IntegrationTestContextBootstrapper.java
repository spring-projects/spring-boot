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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.TestContextBootstrapper;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.context.web.WebMergedContextConfiguration;

/**
 * {@link TestContextBootstrapper} for {@link IntegrationTest}.
 *
 * @author Phillip Webb
 */
class IntegrationTestContextBootstrapper extends SpringBootTestContextBootstrapper {

	private static final String SERVLET_LISTENER = "org.springframework.test.context.web.ServletTestExecutionListener";

	@Override
	protected List<String> getDefaultTestExecutionListenerClassNames() {
		// Remove the ServletTestExecutionListener because it only deals with MockServlet
		List<String> classNames = new ArrayList<String>(
				super.getDefaultTestExecutionListenerClassNames());
		while (classNames.contains(SERVLET_LISTENER)) {
			classNames.remove(SERVLET_LISTENER);
		}
		return Collections.unmodifiableList(classNames);
	}

	@Override
	protected MergedContextConfiguration processMergedContextConfiguration(
			MergedContextConfiguration mergedConfig) {
		mergedConfig = super.processMergedContextConfiguration(mergedConfig);
		WebAppConfiguration webAppConfiguration = AnnotatedElementUtils
				.getMergedAnnotation(mergedConfig.getTestClass(),
						WebAppConfiguration.class);
		if (webAppConfiguration != null) {
			mergedConfig = new WebMergedContextConfiguration(mergedConfig,
					webAppConfiguration.value());
		}
		return mergedConfig;
	}

	@Override
	protected void processPropertySourceProperties(
			MergedContextConfiguration mergedConfig,
			List<String> propertySourceProperties) {
		IntegrationTest annotation = AnnotatedElementUtils
				.getMergedAnnotation(mergedConfig.getTestClass(), IntegrationTest.class);
		propertySourceProperties.addAll(Arrays.asList(annotation.value()));
	}

}
