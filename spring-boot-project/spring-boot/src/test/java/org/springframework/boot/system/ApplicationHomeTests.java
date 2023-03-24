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

package org.springframework.boot.system;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.bytebuddy.ByteBuddy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ApplicationHome}.
 *
 * @author Andy Wilkinson
 */
class ApplicationHomeTests {

	@TempDir
	File tempDir;

	@Test
	void whenSourceClassIsProvidedThenApplicationHomeReflectsItsLocation() throws Exception {
		File app = new File(this.tempDir, "app");
		ApplicationHome applicationHome = createApplicationHome(app);
		assertThat(applicationHome.getDir()).isEqualTo(app);
	}

	@Test
	void whenSourceClassIsProvidedWithSpaceInItsPathThenApplicationHomeReflectsItsLocation() throws Exception {
		File app = new File(this.tempDir, "app location");
		ApplicationHome applicationHome = createApplicationHome(app);
		assertThat(applicationHome.getDir()).isEqualTo(app);
	}

	private ApplicationHome createApplicationHome(File location) throws Exception {
		File examplePackage = new File(location, "com/example");
		examplePackage.mkdirs();
		FileCopyUtils.copy(
				new ByteArrayInputStream(
						new ByteBuddy().subclass(Object.class).name("com.example.Source").make().getBytes()),
				new FileOutputStream(new File(examplePackage, "Source.class")));
		try (URLClassLoader classLoader = new URLClassLoader(new URL[] { location.toURI().toURL() })) {
			Class<?> sourceClass = classLoader.loadClass("com.example.Source");
			// Separate thread to bypass stack-based unit test detection in
			// ApplicationHome
			ExecutorService executor = Executors.newSingleThreadExecutor();
			try {
				return executor.submit(() -> new ApplicationHome(sourceClass)).get();
			}
			finally {
				executor.shutdown();
			}
		}
	}

}
