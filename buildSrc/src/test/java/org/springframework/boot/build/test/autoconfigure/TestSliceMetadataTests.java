/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.test.autoconfigure;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.build.test.autoconfigure.TestSliceMetadata.TestSlice;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TestSliceMetadata}.
 *
 * @author Andy Wilkinson
 */
public class TestSliceMetadataTests {

	@TempDir
	private File temp;

	@Test
	void roundtripJson() {
		TestSliceMetadata source = new TestSliceMetadata("example",
				List.of(new TestSlice("ExampleOneTest", List.of("com.example.OneAutoConfiguration")),
						new TestSlice("ExampleTwoTest", List.of("com.example.TwoAutoConfiguration"))));
		File metadataFile = new File(this.temp, "metadata.json");
		source.writeTo(metadataFile);
		TestSliceMetadata readBack = TestSliceMetadata.readFrom(metadataFile);
		assertThat(source).isEqualTo(readBack);
	}

}
