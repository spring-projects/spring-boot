/*
 * Copyright 2012-2021 the original author or authors.
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
 * Tests for {@link JavaInfo}.
 *
 * @author Jonatan Ivanov
 * @author Stephane Nicoll
 */
class JavaInfoTests {

	@Test
	void javaInfoIsAvailable() {
		JavaInfo javaInfo = new JavaInfo();
		assertThat(javaInfo.getVendor()).isEqualTo(System.getProperty("java.vendor"));
		assertThat(javaInfo.getVersion()).isEqualTo(System.getProperty("java.version"));
		assertThat(javaInfo.getRuntime()).satisfies((jreInfo) -> {
			assertThat(jreInfo.getName()).isEqualTo(System.getProperty("java.runtime.name"));
			assertThat(jreInfo.getVersion()).isEqualTo(System.getProperty("java.runtime.version"));
		});
		assertThat(javaInfo.getJvm()).satisfies((jvmInfo) -> {
			assertThat(jvmInfo.getName()).isEqualTo(System.getProperty("java.vm.name"));
			assertThat(jvmInfo.getVendor()).isEqualTo(System.getProperty("java.vm.vendor"));
			assertThat(jvmInfo.getVersion()).isEqualTo(System.getProperty("java.vm.version"));
		});
	}

}
