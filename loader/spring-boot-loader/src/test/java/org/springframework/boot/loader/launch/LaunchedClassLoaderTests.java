/*
 * Copyright 2012-present the original author or authors.
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
