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

package org.springframework.boot.data.jpa.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DataJpaRepositoriesAutoConfiguration} without Spring Data Envers on
 * the classpath.
 *
 * @author Stefano Cordio
 */
@ClassPathExclusions("spring-data-envers-*.jar")
class DataJpaRepositoriesAutoConfigurationTests extends AbstractDataJpaRepositoriesAutoConfigurationTests {

	@Test
	void autoConfigurationShouldFailWithRevisionRepository() {
		this.contextRunner.withUserConfiguration(RevisionRepositoryConfiguration.class)
			.run((context) -> assertThat(context).getFailure().isInstanceOf(BeanCreationException.class));
	}

}
