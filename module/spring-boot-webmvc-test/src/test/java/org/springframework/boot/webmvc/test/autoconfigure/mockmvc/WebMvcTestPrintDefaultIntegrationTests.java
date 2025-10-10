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

package org.springframework.boot.webmvc.test.autoconfigure.mockmvc;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebMvcTest @WebMvcTest} default print output.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
@ExtendWith(OutputCaptureExtension.class)
@TestMethodOrder(MethodOrderer.MethodName.class)
class WebMvcTestPrintDefaultIntegrationTests {

	@Test
	void shouldNotPrint(CapturedOutput output) {
		executeTests(ShouldNotPrint.class);
		assertThat(output).doesNotContain("HTTP Method");
	}

	@Test
	void shouldPrint(CapturedOutput output) {
		executeTests(ShouldPrint.class);
		assertThat(output).containsOnlyOnce("HTTP Method");
	}

	private void executeTests(Class<?> testClass) {
		LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
			.selectors(DiscoverySelectors.selectClass(testClass))
			.build();
		Launcher launcher = LauncherFactory.create();
		launcher.execute(request);
	}

	@WebMvcTest
	@AutoConfigureMockMvc
	static class ShouldNotPrint {

		@Autowired
		private MockMvcTester mvc;

		@Test
		void test() {
			assertThat(this.mvc.get().uri("/one")).hasStatusOk().hasBodyTextEqualTo("one");
		}

	}

	@WebMvcTest
	@AutoConfigureMockMvc
	static class ShouldPrint {

		@Autowired
		private MockMvcTester mvc;

		@Test
		void test() {
			assertThat(this.mvc.get().uri("/one")).hasStatusOk().hasBodyTextEqualTo("none");
		}

	}

}
