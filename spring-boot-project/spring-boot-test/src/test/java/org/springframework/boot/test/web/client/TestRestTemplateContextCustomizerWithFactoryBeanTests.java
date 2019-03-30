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

package org.springframework.boot.test.web.client;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration tests for {@link TestRestTemplateContextCustomizer} to ensure
 * early-initialization of factory beans doesn't occur.
 *
 * @author Madhura Bhave
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestRestTemplateContextCustomizerWithFactoryBeanTests.TestClassWithFactoryBean.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext
public class TestRestTemplateContextCustomizerWithFactoryBeanTests {

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	public void test() {
	}

	@Configuration
	@ComponentScan("org.springframework.boot.test.web.client.scan")
	static class TestClassWithFactoryBean {

		@Bean
		public TomcatServletWebServerFactory webServerFactory() {
			return new TomcatServletWebServerFactory(0);
		}

	}

}
