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

package sample.propertyvalidation;

import org.junit.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.Validator;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link SamplePropertyValidationApplication}.
 *
 * @author Lucas Saldanha
 */
public class SamplePropertyValidationApplicationTests {

	private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@Test
	public void testBindingValidProperties() {
		this.context.register(TestConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context, "host:192.168.0.1");
		EnvironmentTestUtils.addEnvironment(this.context, "port:8080");
		this.context.refresh();

		assertEquals(1, this.context.getBeanNamesForType(SampleProperties.class).length);
		SampleProperties properties = this.context.getBean(SampleProperties.class);
		assertEquals("192.168.0.1", properties.getHost());
		assertEquals(8080, (int) properties.getPort());
	}

	@Test(expected = BeanCreationException.class)
	public void testBindingInvalidProperties() {
		this.context.register(TestConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context, "host:xxxxxx");
		EnvironmentTestUtils.addEnvironment(this.context, "port:8080");
		this.context.refresh();
	}

	@Test
	public void testBindingValidPropertiesWithMultipleConfigurationPropertiesClasses() {
		this.context.register(TestConfiguration.class);
		this.context.register(ServerProperties.class);
		EnvironmentTestUtils.addEnvironment(this.context, "host:192.168.0.1");
		EnvironmentTestUtils.addEnvironment(this.context, "port:8080");
		this.context.refresh();

		assertEquals(1, this.context.getBeanNamesForType(SampleProperties.class).length);
		SampleProperties properties = this.context.getBean(SampleProperties.class);
		assertEquals("192.168.0.1", properties.getHost());
		assertEquals(8080, (int) properties.getPort());
	}

	@Configuration
	@EnableConfigurationProperties(SampleProperties.class)
	protected static class TestConfiguration {

		@Bean
		public Validator configurationPropertiesValidator() {
			return new ConfigurationPropertiesValidator();
		}
	}

}
