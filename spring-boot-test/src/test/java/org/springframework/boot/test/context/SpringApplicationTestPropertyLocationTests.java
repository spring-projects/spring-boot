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

import javax.annotation.PostConstruct;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringApplicationTest.WebEnvironment;
import org.springframework.boot.test.context.SpringApplicationTestPropertyLocationTests.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.Environment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringApplicationTest} with {@link TestPropertySource} locations.
 *
 * @author Phillip Webb
 */
@RunWith(SpringRunner.class)
@DirtiesContext
@SpringApplicationConfiguration(Config.class)
@SpringApplicationTest(webEnvironment = WebEnvironment.NONE, properties = "value1=123")
@TestPropertySource(properties = "value2=456", locations = "classpath:/test-property-source-annotation.properties")
public class SpringApplicationTestPropertyLocationTests {

	@Autowired
	private Environment environment;

	@Test
	public void loadedProperties() throws Exception {
		assertThat(this.environment.getProperty("value1")).isEqualTo("123");
		assertThat(this.environment.getProperty("value2")).isEqualTo("456");
		assertThat(this.environment.getProperty("annotation-referenced"))
				.isEqualTo("fromfile");
	}

	@Configuration
	static class Config {

		@Value("${value1}")
		private String value1;

		@Value("${value2}")
		private String value2;

		@Value("${annotation-referenced}")
		private String annotationReferenced;

		@PostConstruct
		void checkValues() {
			assertThat(this.value1).isEqualTo("123");
			assertThat(this.value2).isEqualTo("456");
			assertThat(this.annotationReferenced).isEqualTo("fromfile");
		}

		@Bean
		public static PropertySourcesPlaceholderConfigurer propertyPlaceholder() {
			return new PropertySourcesPlaceholderConfigurer();
		}

	}

}
