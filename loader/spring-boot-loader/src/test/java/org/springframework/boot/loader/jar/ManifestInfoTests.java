/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.loader.jar;

import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ManifestInfo}.
 *
 * @author Phillip Webb
 */
class ManifestInfoTests {

	@Test
	void noneReturnsNoDetails() {
		assertThat(ManifestInfo.NONE.getManifest()).isNull();
		assertThat(ManifestInfo.NONE.isMultiRelease()).isFalse();
	}

	@Test
	void getManifestReturnsManifest() {
		Manifest manifest = new Manifest();
		ManifestInfo info = new ManifestInfo(manifest);
		assertThat(info.getManifest()).isSameAs(manifest);
	}

	@Test
	void isMultiReleaseWhenHasMultiReleaseAttributeReturnsTrue() {
		Manifest manifest = new Manifest();
		manifest.getMainAttributes().put(new Name("Multi-Release"), "true");
		ManifestInfo info = new ManifestInfo(manifest);
		assertThat(info.isMultiRelease()).isTrue();
	}

	@Test
	void isMultiReleaseWhenHasNoMultiReleaseAttributeReturnsFalse() {
		Manifest manifest = new Manifest();
		manifest.getMainAttributes().put(new Name("Random-Release"), "true");
		ManifestInfo info = new ManifestInfo(manifest);
		assertThat(info.isMultiRelease()).isFalse();
	}

}
