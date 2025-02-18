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
