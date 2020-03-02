/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.hateoas;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.boot.test.autoconfigure.hateoas.HypermediaTestRestTemplateTests.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.hateoas.config.EnableHypermediaSupport.HypermediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(classes = TestConfiguration.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext
public class HypermediaTestRestTemplateTests {

	@Autowired
	private TestRestTemplate testRestTemplate;

	@Test
	void testRestTemplateShouldHaveHypermediaSupportWiredIn() {
		assertThat(this.testRestTemplate.getRestTemplate().getMessageConverters())
				.flatExtracting(HttpMessageConverter::getSupportedMediaTypes)
				.contains(MediaTypes.HAL_JSON, MediaTypes.COLLECTION_JSON)
				.doesNotContain(MediaTypes.HAL_FORMS_JSON, MediaTypes.UBER_JSON);
	}

	@ImportAutoConfiguration({ HypermediaTestAutoConfiguration.class, RestTemplateAutoConfiguration.class })
	@Configuration(proxyBeanMethods = false)
	@EnableHypermediaSupport(type = HypermediaType.HAL)
	static class TestConfiguration {

	}

}
