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

package org.springframework.boot.test.context;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.AbstractSpringBootTestWebServerWebEnvironmentTests.AbstractConfig;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link SpringBootTest @SpringBootTest} with a custom inline server.port in an
 * embedded web environment.
 *
 * @author Stephane Nicoll
 */
@DirtiesContext
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = { "server.port=12345" })
class SpringBootTestWebEnvironmentRandomPortCustomPortTests {

	@Autowired
	private Environment environment;

	@Test
	void validatePortIsNotOverwritten() {
		String port = this.environment.getProperty("server.port");
		assertThat(port).isEqualTo("0");
	}

	@Configuration(proxyBeanMethods = false)
	@EnableWebMvc
	protected static class Config extends AbstractConfig {

	}

}
