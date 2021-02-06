/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.actuate.libraries;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.libraries.Libraries.Builder;
import org.springframework.core.io.InputStreamResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link BasicLibrariesContributor}.
 *
 * @author Phil Clay
 */
class BasicLibrariesContributorTests {

	@Test
	void contribute() {
		Libraries librariesToContribute = Libraries.builder()
				.addLibrary("categoryA", Collections.singletonMap("key1", "value1"))
				.addLibrary("categoryA", Collections.singletonMap("key2", "value2"))
				.addLibrary("categoryB", Collections.singletonMap("key3", "value3")).build();
		BasicLibrariesContributor contributor = new BasicLibrariesContributor(librariesToContribute);

		Builder targetBuilder = Libraries.builder();
		contributor.contribute(targetBuilder);

		Libraries librariesContributed = targetBuilder.build();

		Map<String, List<Map<String, Object>>> details = librariesContributed.getDetails();

		assertThat(details).containsExactly(
				entry("categoryA",
						Arrays.asList(Collections.singletonMap("key1", "value1"),
								Collections.singletonMap("key2", "value2"))),
				entry("categoryB", Arrays.asList(Collections.singletonMap("key3", "value3"))));
	}

	@Test
	void fromYaml() throws IOException {
		String yaml = "- key1: value1\n" + "- key2: value2\n";
		LibrariesContributor contributor = BasicLibrariesContributor.fromYamlResource("categoryA",
				new InputStreamResource(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8))));

		Builder targetBuilder = Libraries.builder();
		contributor.contribute(targetBuilder);

		Libraries librariesContributed = targetBuilder.build();

		Map<String, List<Map<String, Object>>> details = librariesContributed.getDetails();

		assertThat(details).containsExactly(entry("categoryA",
				Arrays.asList(Collections.singletonMap("key1", "value1"), Collections.singletonMap("key2", "value2"))));
	}

}
