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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringBootTest} configured with specific classes.
 *
 * @author Phillip Webb
 */
@RunWith(SpringRunner.class)
@DirtiesContext
@SpringBootTest(classes = SpringBootTestWithClassesIntegrationTests.Config.class)
public class SpringBootTestWithClassesIntegrationTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Autowired
	private ApplicationContext context;

	@Test
	public void injectsOnlyConfig() throws Exception {
		assertThat(this.context.getBean(Config.class)).isNotNull();
		this.thrown.expect(NoSuchBeanDefinitionException.class);
		this.context.getBean(AdditionalConfig.class);
	}

	@Configuration
	static class Config {

	}

	@Configuration
	static class AdditionalConfig {

	}

}
