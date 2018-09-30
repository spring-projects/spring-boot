/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.docs.test.spock;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.LocalHostUriTemplateHandler;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Example configuration for using TestRestTemplate with Spock 1.0 when
 * {@link SpringBootTest} cannot be used.
 *
 * @author Andy Wilkinson
 */
public class SpockTestRestTemplateExample {

	/**
	 * Test configuration for a {@link TestRestTemplate}.
	 */
	// tag::test-rest-template-configuration[]
	@Configuration
	static class TestRestTemplateConfiguration {

		@Bean
		public TestRestTemplate testRestTemplate(
				ObjectProvider<RestTemplateBuilder> builderProvider,
				Environment environment) {
			RestTemplateBuilder builder = builderProvider.getIfAvailable();
			TestRestTemplate template = (builder != null) ? new TestRestTemplate(builder)
					: new TestRestTemplate();
			template.setUriTemplateHandler(new LocalHostUriTemplateHandler(environment));
			return template;
		}

	}
	// end::test-rest-template-configuration[]

}
