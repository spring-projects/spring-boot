/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.boot.testsupport.classpath.ForkedClassPath;
import org.springframework.context.support.StaticApplicationContext;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@ForkedClassPath
class IgnoringXmlBeanDefinitionLoaderTests {

	@BeforeAll
	static void ignoreXml() {
		System.setProperty("spring.xml.ignore", "true");
	}

	@AfterAll
	static void enableXml() {
		System.clearProperty("spring.xml.ignore");
	}

	@Test
	void whenXmlSupportIsDisabledXmlSourcesAreRejected() {
		assertThatExceptionOfType(BeanDefinitionStoreException.class)
				.isThrownBy(() -> new BeanDefinitionLoader(new StaticApplicationContext(),
						"classpath:org/springframework/boot/sample-beans.xml").load())
				.withMessage("Cannot load XML bean definitions when XML support is disabled");
	}

}
