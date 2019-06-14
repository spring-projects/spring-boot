/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.context.properties.bind;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DataObjectPropertyName}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class DataObjectPropertyNameTests {

	@Test
	void toDashedCaseShouldConvertValue() {
		assertThat(DataObjectPropertyName.toDashedForm("Foo")).isEqualTo("foo");
		assertThat(DataObjectPropertyName.toDashedForm("foo")).isEqualTo("foo");
		assertThat(DataObjectPropertyName.toDashedForm("fooBar")).isEqualTo("foo-bar");
		assertThat(DataObjectPropertyName.toDashedForm("foo_bar")).isEqualTo("foo-bar");
		assertThat(DataObjectPropertyName.toDashedForm("_foo_bar")).isEqualTo("-foo-bar");
		assertThat(DataObjectPropertyName.toDashedForm("foo_Bar")).isEqualTo("foo-bar");
	}

}
