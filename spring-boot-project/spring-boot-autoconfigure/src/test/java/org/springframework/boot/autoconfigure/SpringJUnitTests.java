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

package org.springframework.boot.autoconfigure;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Dave Syer
 */
@DirtiesContext
@SpringBootTest
@RunWith(SpringRunner.class)
public class SpringJUnitTests {

	@Autowired
	private ApplicationContext context;

	@Value("${foo:spam}")
	private String foo = "bar";

	@Test
	public void testContextCreated() {
		assertThat(this.context).isNotNull();
	}

	@Test
	public void testContextInitialized() {
		assertThat(this.foo).isEqualTo("bucket");
	}

	@Configuration
	@Import({ PropertyPlaceholderAutoConfiguration.class })
	public static class TestConfiguration {

	}

}
