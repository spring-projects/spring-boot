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

import org.springframework.boot.test.web.client.LocalHostUriTemplateHandler;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;

/**
 * {@link ContextCustomizer} for {@link WebIntegrationTest} that provides a
 * {@link TestRestTemplate} bean that can automatically resolve
 * {@literal local.server.port}.
 *
 * @author Phillip Webb
 */
class WebIntegrationTestContextCustomizer implements ContextCustomizer {

	@Override
	public void customizeContext(ConfigurableApplicationContext context,
			MergedContextConfiguration mergedContextConfiguration) {
		TestRestTemplate restTemplate = getRestTemplate(context.getEnvironment());
		context.getBeanFactory().registerSingleton("testRestTemplate", restTemplate);
	}

	private TestRestTemplate getRestTemplate(Environment environment) {
		TestRestTemplate template = new TestRestTemplate();
		template.setUriTemplateHandler(new LocalHostUriTemplateHandler(environment));
		return template;
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

}
