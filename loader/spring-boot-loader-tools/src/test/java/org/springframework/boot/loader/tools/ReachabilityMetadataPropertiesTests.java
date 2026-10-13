/*
 * Copyright 2012-present the original author or authors.
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
