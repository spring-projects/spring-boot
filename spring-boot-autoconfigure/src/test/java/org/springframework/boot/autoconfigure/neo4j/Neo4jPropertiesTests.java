/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.autoconfigure.neo4j;

import org.assertj.core.api.Assertions;

import org.junit.Test;

import org.springframework.boot.autoconfigure.data.neo4j.Neo4jProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;

/**
 * Tests for {@link Neo4jProperties}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Vince Bickers
 */

public class Neo4jPropertiesTests {

	@Test
	public void shouldHaveCorrectDefaultDriver() {

		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(Conf.class);
		context.refresh();

		Neo4jProperties neo4jProperties = context.getBean(Neo4jProperties.class);

		Assertions.assertThat(neo4jProperties.getDriver()).isEqualTo("org.neo4j.ogm.drivers.embedded.driver.EmbeddedDriver");
	}

	@Test
	public void shouldConfigureFromDefaults()  {

		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(Conf.class);
		context.refresh();

		Neo4jProperties neo4jProperties = context.getBean(Neo4jProperties.class);

		org.neo4j.ogm.config.Configuration configuration = neo4jProperties.configure();
		Assertions.assertThat(configuration.driverConfiguration().getDriverClassName())
		.isEqualTo("org.neo4j.ogm.drivers.embedded.driver.EmbeddedDriver");

	}

	@Test
	public void shouldBeCustomisable() {

		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(context, "spring.data.neo4j.driver:CustomDriver");
		context.register(Conf.class);
		context.refresh();

		Neo4jProperties neo4jProperties = context.getBean(Neo4jProperties.class);

		Assertions.assertThat(neo4jProperties.getDriver()).isEqualTo("CustomDriver");

	}

	@Configuration
	@EnableConfigurationProperties(Neo4jProperties.class)
	static class Conf {
	}
}
