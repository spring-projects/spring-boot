/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.testcontainers.service.connection;

import org.junit.jupiter.api.Test;

import org.springframework.boot.origin.Origin;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link FieldOrigin}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class FieldOriginTests {

	@Test
	void createWhenFieldIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new FieldOrigin(null))
			.withMessage("Field must not be null");
	}

	@Test
	void equalsAndHashCode() {
		Origin o1 = new FieldOrigin(ReflectionUtils.findField(Fields.class, "one"));
		Origin o2 = new FieldOrigin(ReflectionUtils.findField(Fields.class, "one"));
		Origin o3 = new FieldOrigin(ReflectionUtils.findField(Fields.class, "two"));
		assertThat(o1).isEqualTo(o1).isEqualTo(o2).isNotEqualTo(o3);
		assertThat(o1.hashCode()).isEqualTo(o2.hashCode());
	}

	@Test
	void toStringReturnsSensibleString() {
		Origin origin = new FieldOrigin(ReflectionUtils.findField(Fields.class, "one"));
		assertThat(origin).hasToString("FieldOriginTests.Fields.one");
	}

	static class Fields {

		String one;

		String two;

	}

}
