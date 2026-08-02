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

package org.springframework.boot.jackson.autoconfigure.xmltest;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jackson.autoconfigure.xmltest.app.ExampleXmlApplication;
import org.springframework.boot.jackson.autoconfigure.xmltest.app.ExampleXmlJacksonComponent;
import org.springframework.boot.jackson.autoconfigure.xmltest.app.ExampleXmlService;
import org.springframework.boot.test.autoconfigure.xml.XmlTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the component scanning applied by {@link XmlTest @XmlTest}.
 *
 * @author Tiziano Basile
 */
@XmlTest
@ContextConfiguration(classes = ExampleXmlApplication.class)
class XmlTestTypeExcludeFilterTests {

	@Autowired
	private ApplicationContext context;

	@Test
	void componentScanningWhenJacksonComponentThenBeanIsRegistered() {
		assertThat(this.context.getBeansOfType(ExampleXmlJacksonComponent.class)).hasSize(1);
	}

	@Test
	void componentScanningWhenStandardComponentThenBeanIsNotRegistered() {
		assertThat(this.context.getBeansOfType(ExampleXmlService.class)).isEmpty();
	}

}
