/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testsupport.classpath;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ForkedClassPath @ForkedClassPath}.
 *
 * @author Christoph Dreis
 */
@ForkedClassPath
class ModifiedClassPathExtensionForkTests {

	@Test
	void modifiedClassLoaderIsUsed() {
		ClassLoader classLoader = getClass().getClassLoader();
		assertThat(classLoader.getClass().getName()).isEqualTo(ModifiedClassPathClassLoader.class.getName());
	}

}
