/*
 * Copyright 2012-2017 the original author or authors.
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

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link SpringBootTest} with a custom inline server.port in a non-embedded web
 * environment.
 *
 * @author Stephane Nicoll
 */
@RunWith(SpringRunner.class)
@SpringBootTest(properties = "server.port=12345")
public class SpringBootTestCustomPortTests {

	@Autowired
	private Environment environment;

	@Test
	public void validatePortIsNotOverwritten() {
		String port = this.environment.getProperty("server.port");
		assertThat(port).isEqualTo("12345");
	}

	@Configuration
	protected static class Config {

	}

}
