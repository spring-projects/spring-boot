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

import org.springframework.boot.test.web.client.LocalHostUriTemplateHandler;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;

/**
 * {@link ContextCustomizer} for {@link SpringBootTest}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class SpringBootTestContextCustomizer implements ContextCustomizer {

	@Override
	public void customizeContext(ConfigurableApplicationContext context,
			MergedContextConfiguration mergedContextConfiguration) {
		SpringBootTest annotation = AnnotatedElementUtils.getMergedAnnotation(
				mergedContextConfiguration.getTestClass(), SpringBootTest.class);
		if (annotation.webEnvironment().isEmbedded()) {
			Object restTemplate = TestRestTemplateFactory
					.createRestTemplate(context.getEnvironment());
			context.getBeanFactory().registerSingleton("testRestTemplate", restTemplate);
		}
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || obj.getClass() != getClass()) {
			return false;
		}
		return true;
	}

	// Inner class to avoid references to web classes that may not be on the classpath
	private static class TestRestTemplateFactory {

		private static TestRestTemplate createRestTemplate(Environment environment) {
			TestRestTemplate template = new TestRestTemplate();
			template.setUriTemplateHandler(new LocalHostUriTemplateHandler(environment));
			return template;
		}

	}

}
