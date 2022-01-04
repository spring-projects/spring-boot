/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.info;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OsInfo}.
 *
 * @author Jonatan Ivanov
 */
public class OsInfoTests {

	@Test
	void osInfoIsAvailable() {
		OsInfo osInfo = new OsInfo();
		assertThat(osInfo.getName()).isEqualTo(System.getProperty("os.name"));
		assertThat(osInfo.getVersion()).isEqualTo(System.getProperty("os.version"));
		assertThat(osInfo.getArch()).isEqualTo(System.getProperty("os.arch"));
	}

}
