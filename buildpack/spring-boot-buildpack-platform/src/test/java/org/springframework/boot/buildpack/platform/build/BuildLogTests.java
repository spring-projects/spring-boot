/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.build;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BuildLog}.
 *
 * @author Phillip Webb
 */
class BuildLogTests {

	@Test
	void toSystemOutPrintsToSystemOut() {
		BuildLog log = BuildLog.toSystemOut();
		assertThat(log).isInstanceOf(PrintStreamBuildLog.class);
		assertThat(log).extracting("out").isSameAs(System.out);
	}

	@Test
	void toPrintsToOutput() {
		BuildLog log = BuildLog.to(System.err);
		assertThat(log).isInstanceOf(PrintStreamBuildLog.class);
		assertThat(log).extracting("out").isSameAs(System.err);
	}

}
