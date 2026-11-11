/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.loader.net.protocol.jar;

import java.util.jar.Attributes;
import java.util.jar.JarEntry;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link UrlJarEntry}.
 *
 * @author Phillip Webb
 */
class UrlJarEntryTests {

	@Test
	void ofWhenEntryIsNullReturnsNull() {
		assertThat(UrlJarEntry.of(null, null)).isNull();
	}

	@Test
	void ofReturnsUrlJarEntry() {
		JarEntry entry = new JarEntry("test");
		assertThat(UrlJarEntry.of(entry, null)).isNotNull();

	}

	@Test
	void getAttributesDelegatesToUrlJarManifest() throws Exception {
		JarEntry entry = new JarEntry("test");
		UrlJarManifest manifest = mock(UrlJarManifest.class);
		Attributes attributes = mock(Attributes.class);
		given(manifest.getEntryAttributes(any())).willReturn(attributes);
		assertThat(UrlJarEntry.of(entry, manifest).getAttributes()).isSameAs(attributes);
	}

}
