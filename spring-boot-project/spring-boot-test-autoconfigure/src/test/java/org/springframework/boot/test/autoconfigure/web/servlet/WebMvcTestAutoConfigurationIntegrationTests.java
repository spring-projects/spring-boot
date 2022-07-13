/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.web.servlet;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration;
import org.springframework.boot.autoconfigure.groovy.template.GroovyTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.mustache.MustacheAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.HttpEncodingAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.AutoConfigurationImportedCondition.importedAutoConfiguration;

/**
 * Tests for the auto-configuration imported by {@link WebMvcTest @WebMvcTest}.
 *
 * @author Andy Wilkinson
 * @author Levi Puot Paul
 * @author Madhura Bhave
 */
@WebMvcTest
class WebMvcTestAutoConfigurationIntegrationTests {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void freemarkerAutoConfigurationWasImported() {
		assertThat(this.applicationContext).has(importedAutoConfiguration(FreeMarkerAutoConfiguration.class));
	}

	@Test
	void groovyTemplatesAutoConfigurationWasImported() {
		assertThat(this.applicationContext).has(importedAutoConfiguration(GroovyTemplateAutoConfiguration.class));
	}

	@Test
	void mustacheAutoConfigurationWasImported() {
		assertThat(this.applicationContext).has(importedAutoConfiguration(MustacheAutoConfiguration.class));
	}

	@Test
	void thymeleafAutoConfigurationWasImported() {
		assertThat(this.applicationContext).has(importedAutoConfiguration(ThymeleafAutoConfiguration.class));
	}

	@Test
	void taskExecutionAutoConfigurationWasImported() {
		assertThat(this.applicationContext).has(importedAutoConfiguration(TaskExecutionAutoConfiguration.class));
	}

	@Test
	void asyncTaskExecutorWithApplicationTaskExecutor() {
		assertThat(this.applicationContext.getBeansOfType(AsyncTaskExecutor.class)).hasSize(1);
		assertThat(this.applicationContext.getBean(RequestMappingHandlerAdapter.class)).extracting("taskExecutor")
				.isSameAs(this.applicationContext.getBean("applicationTaskExecutor"));
	}

	@Test
	void oAuth2ClientAutoConfigurationWasImported() {
		assertThat(this.applicationContext).has(importedAutoConfiguration(OAuth2ClientAutoConfiguration.class));
	}

	@Test
	void oAuth2ResourceServerAutoConfigurationWasImported() {
		assertThat(this.applicationContext).has(importedAutoConfiguration(OAuth2ResourceServerAutoConfiguration.class));
	}

	@Test
	void httpEncodingAutoConfigurationWasImported() {
		assertThat(this.applicationContext).has(importedAutoConfiguration(HttpEncodingAutoConfiguration.class));
	}

}
