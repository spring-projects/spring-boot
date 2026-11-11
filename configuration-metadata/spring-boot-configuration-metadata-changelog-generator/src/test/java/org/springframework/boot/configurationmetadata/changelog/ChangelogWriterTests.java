/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationmetadata.changelog;

import java.io.File;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import org.assertj.core.util.Files;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ChangelogWriter}.
 *
 * @author Phillip Webb
 */
class ChangelogWriterTests {

	@Test
	void writeChangelog() {
		StringWriter out = new StringWriter();
		try (ChangelogWriter writer = new ChangelogWriter(out)) {
			writer.write(TestChangelog.load());
		}
		String expected = Files.contentOf(new File("src/test/resources/sample.adoc"), StandardCharsets.UTF_8);
		assertThat(out).hasToString(expected);
	}

}
