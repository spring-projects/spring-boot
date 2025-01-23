/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.loader.launch;

import java.net.URL;
import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.loader.zip.AssertFileChannelDataBlocksClosed;
import org.springframework.boot.testsupport.system.CapturedOutput;
import org.springframework.boot.testsupport.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Launcher}.
 *
 * @author Phillip Webb
 */
@ExtendWith(OutputCaptureExtension.class)
@AssertFileChannelDataBlocksClosed
class LauncherTests {

	/**
	 * Jar Mode tests.
	 */
	@Nested
	class JarMode {

		@BeforeEach
		void setup() {
			System.setProperty(JarModeRunner.DISABLE_SYSTEM_EXIT, "true");
		}

		@AfterEach
		void cleanup() {
			System.clearProperty("jarmode");
			System.clearProperty(JarModeRunner.DISABLE_SYSTEM_EXIT);
		}

		@Test
		void launchWhenJarModePropertyIsSetLaunchesJarMode(CapturedOutput out) throws Exception {
			System.setProperty("jarmode", "test");
			new TestLauncher().launch(new String[] { "boot" });
			assertThat(out).contains("running in test jar mode [boot]");
			assertThat(System.getProperty(JarModeRunner.SUPPRESSED_SYSTEM_EXIT_CODE)).isEqualTo("0");
		}

		@Test
		void launchWhenJarModePropertyIsNotAcceptedThrowsException(CapturedOutput out) throws Exception {
			System.setProperty("jarmode", "idontexist");
			new TestLauncher().launch(new String[] { "boot" });
			assertThat(out).contains("Unsupported jarmode 'idontexist'");
			assertThat(System.getProperty(JarModeRunner.SUPPRESSED_SYSTEM_EXIT_CODE)).isEqualTo("1");
		}

		@Test
		void launchWhenJarModeRunFailsWithErrorExceptionPrintsSimpleMessage(CapturedOutput out) throws Exception {
			System.setProperty("jarmode", "test");
			new TestLauncher().launch(new String[] { "error" });
			assertThat(out).contains("running in test jar mode [error]");
			assertThat(out).contains("Error: error message");
			assertThat(System.getProperty(JarModeRunner.SUPPRESSED_SYSTEM_EXIT_CODE)).isEqualTo("1");
		}

		@Test
		void launchWhenJarModeRunFailsWithErrorExceptionPrintsStackTrace(CapturedOutput out) throws Exception {
			System.setProperty("jarmode", "test");
			new TestLauncher().launch(new String[] { "fail" });
			assertThat(out).contains("running in test jar mode [fail]");
			assertThat(out).contains("java.lang.IllegalStateException: bad");
			assertThat(System.getProperty(JarModeRunner.SUPPRESSED_SYSTEM_EXIT_CODE)).isEqualTo("1");
		}

	}

	private static final class TestLauncher extends Launcher {

		@Override
		protected String getMainClass() throws Exception {
			throw new IllegalStateException("Should not be called");
		}

		@Override
		protected Archive getArchive() {
			return null;
		}

		@Override
		protected Set<URL> getClassPathUrls() throws Exception {
			return Collections.emptySet();
		}

		@Override
		protected void launch(String[] args) throws Exception {
			super.launch(args);
		}

	}

}
