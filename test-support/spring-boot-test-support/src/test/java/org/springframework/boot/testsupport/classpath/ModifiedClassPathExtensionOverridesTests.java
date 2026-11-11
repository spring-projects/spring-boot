/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testsupport.classpath;

import org.junit.jupiter.api.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ModifiedClassPathExtension} overriding entries on the class path.
 *
 * @author Christoph Dreis
 */
@ClassPathOverrides("org.springframework:spring-context:4.1.0.RELEASE")
class ModifiedClassPathExtensionOverridesTests {

	@Test
	void classesAreLoadedFromOverride() {
		assertThat(ApplicationContext.class.getProtectionDomain().getCodeSource().getLocation().toString())
			.endsWith("spring-context-4.1.0.RELEASE.jar");
	}

	@Test
	void classesAreLoadedFromTransitiveDependencyOfOverride() {
		assertThat(StringUtils.class.getProtectionDomain().getCodeSource().getLocation().toString())
			.endsWith("spring-core-4.1.0.RELEASE.jar");
	}

}
