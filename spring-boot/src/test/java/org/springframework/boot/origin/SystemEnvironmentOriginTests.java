/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.origin;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SystemEnvironmentOrigin}.
 *
 * @author Madhura Bhave
 */
public class SystemEnvironmentOriginTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void createWhenPropertyIsNullShouldThrowException() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		new SystemEnvironmentOrigin(null);
	}

	@Test
	public void createWhenPropertyNameIsEmptyShouldThrowException() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		new SystemEnvironmentOrigin("");
	}

	@Test
	public void getPropertyShouldReturnProperty() throws Exception {
		SystemEnvironmentOrigin origin = new SystemEnvironmentOrigin("FOO_BAR");
		assertThat(origin.getProperty()).isEqualTo("FOO_BAR");
	}

	@Test
	public void toStringShouldReturnStringWithDetails() throws Exception {
		SystemEnvironmentOrigin origin = new SystemEnvironmentOrigin("FOO_BAR");
		assertThat(origin.toString())
				.isEqualTo("System Environment Property \"FOO_BAR\"");
	}
}
