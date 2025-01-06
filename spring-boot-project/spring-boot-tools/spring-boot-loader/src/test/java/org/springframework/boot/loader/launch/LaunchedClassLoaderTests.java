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

package org.springframework.boot.loader.launch;

import java.net.URL;

import org.junit.jupiter.api.Test;

import org.springframework.boot.loader.jarmode.JarMode;
import org.springframework.boot.loader.zip.AssertFileChannelDataBlocksClosed;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LaunchedClassLoader}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
@AssertFileChannelDataBlocksClosed
class LaunchedClassLoaderTests {

	@Test
	void loadClassWhenJarModeClassLoadsInLaunchedClassLoader() throws Exception {
		try (LaunchedClassLoader classLoader = new LaunchedClassLoader(false, new URL[] {},
				getClass().getClassLoader())) {
			Class<?> jarModeClass = classLoader.loadClass(JarMode.class.getName());
			Class<?> jarModeRunnerClass = classLoader.loadClass(JarModeRunner.class.getName());
			assertThat(jarModeClass.getClassLoader()).isSameAs(classLoader);
			assertThat(jarModeRunnerClass.getClassLoader()).isSameAs(classLoader);
		}
	}

}
