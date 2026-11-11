/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.info;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OsInfo}.
 *
 * @author Jonatan Ivanov
 */
class OsInfoTests {

	@Test
	void osInfoIsAvailable() {
		OsInfo osInfo = new OsInfo();
		assertThat(osInfo.getName()).isEqualTo(System.getProperty("os.name"));
		assertThat(osInfo.getVersion()).isEqualTo(System.getProperty("os.version"));
		assertThat(osInfo.getArch()).isEqualTo(System.getProperty("os.arch"));
	}

}
