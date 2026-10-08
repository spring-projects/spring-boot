/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.test.autoconfigure.xml;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.xml.XmlTestersAutoConfiguration.XmlMarshalTestersBeanPostProcessor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link SpringBootTest @SpringBootTest} with
 * {@link AutoConfigureXmlTesters @AutoConfigureXmlTesters}. Proves that the
 * {@code spring.test.xmltesters} property mapping is correct, as
 * {@link XmlTestersAutoConfiguration} would otherwise back off silently.
 *
 * @author Tiziano Basile
 */
@SpringBootTest
@AutoConfigureXmlTesters
@ContextConfiguration(classes = ExampleXmlApplication.class)
class SpringBootTestWithAutoConfigureXmlTestersTests {

	private final ExampleXmlMarshalTester<String> xml = new ExampleXmlMarshalTester<>();

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private Environment environment;

	@Test
	void propertyMappingWhenAutoConfigureXmlTestersThenTestersAreEnabled() {
		assertThat(this.environment.getProperty("spring.test.xmltesters.enabled")).isEqualTo("true");
	}

	@Test
	void contextWhenAutoConfigureXmlTestersThenBeanPostProcessorIsRegistered() {
		assertThat(this.applicationContext.getBeansOfType(XmlMarshalTestersBeanPostProcessor.class)).hasSize(1);
	}

	@Test
	void testerWhenAutoConfigureXmlTestersThenIsInitialized() {
		assertThat(this.xml.isInitialized()).isTrue();
		assertThat(this.xml.getTypeUnderTest().resolve()).isEqualTo(String.class);
	}

}
