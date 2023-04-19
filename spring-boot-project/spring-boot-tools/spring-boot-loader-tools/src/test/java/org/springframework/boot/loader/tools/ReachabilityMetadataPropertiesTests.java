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

package org.springframework.boot.loader.tools;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ReachabilityMetadataProperties}.
 *
 * @author Moritz Halbritter
 */
class ReachabilityMetadataPropertiesTests {

	@Test
	void shouldReadFromInputStream() throws IOException {
		String propertiesContent = "override=true\n";
		ReachabilityMetadataProperties properties = ReachabilityMetadataProperties
			.fromInputStream(new ByteArrayInputStream(propertiesContent.getBytes(StandardCharsets.UTF_8)));
		assertThat(properties.isOverridden()).isTrue();
	}

	@Test
	void shouldFormatLocation() {
		String location = ReachabilityMetadataProperties
			.getLocation(LibraryCoordinates.of("group-id", "artifact-id", "1.0.0"));
		assertThat(location)
			.isEqualTo("META-INF/native-image/group-id/artifact-id/1.0.0/reachability-metadata.properties");
	}

}
