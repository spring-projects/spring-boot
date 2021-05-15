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

package org.springframework.boot.loader.jarmode;

import java.util.Collections;
import java.util.Iterator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.loader.Launcher;
import org.springframework.boot.loader.archive.Archive;
import org.springframework.boot.testsupport.system.CapturedOutput;
import org.springframework.boot.testsupport.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Launcher} with jar mode support.
 *
 * @author Phillip Webb
 */
@ExtendWith(OutputCaptureExtension.class)
class LauncherJarModeTests {

	@BeforeEach
	void setup() {
		System.setProperty(JarModeLauncher.DISABLE_SYSTEM_EXIT, "true");
	}

	@AfterEach
	void cleanup() {
		System.clearProperty("jarmode");
		System.clearProperty(JarModeLauncher.DISABLE_SYSTEM_EXIT);
	}

	@Test
	void launchWhenJarModePropertyIsSetLaunchesJarMode(CapturedOutput out) throws Exception {
		System.setProperty("jarmode", "test");
		new TestLauncher().launch(new String[] { "boot" });
		assertThat(out).contains("running in test jar mode [boot]");
	}

	@Test
	void launchWhenJarModePropertyIsNotAcceptedThrowsException(CapturedOutput out) throws Exception {
		System.setProperty("jarmode", "idontexist");
		new TestLauncher().launch(new String[] { "boot" });
		assertThat(out).contains("Unsupported jarmode 'idontexist'");
	}

	private static class TestLauncher extends Launcher {

		@Override
		protected String getMainClass() throws Exception {
			throw new IllegalStateException("Should not be called");
		}

		@Override
		protected Iterator<Archive> getClassPathArchivesIterator() throws Exception {
			return Collections.emptyIterator();
		}

		@Override
		protected void launch(String[] args) throws Exception {
			super.launch(args);
		}

	}

}
