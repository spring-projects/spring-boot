/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.loader.net.protocol.jar;

import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.Manifest;

import org.junit.jupiter.api.Test;

import org.springframework.boot.loader.net.protocol.jar.UrlJarManifest.ManifestSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

/**
 * Tests for {@link UrlJarManifest}.
 *
 * @author Phillip Webb
 */
class UrlJarManifestTests {

	@Test
	void getWhenSuppliedManifestIsNullReturnsNull() throws Exception {
		UrlJarManifest urlJarManifest = new UrlJarManifest(() -> null);
		assertThat(urlJarManifest.get()).isNull();
	}

	@Test
	void getAlwaysReturnsDeepCopy() throws Exception {
		Manifest manifest = new Manifest();
		UrlJarManifest urlJarManifest = new UrlJarManifest(() -> manifest);
		manifest.getMainAttributes().putValue("test", "one");
		manifest.getEntries().put("spring", new Attributes());
		Manifest copy = urlJarManifest.get();
		assertThat(copy).isNotSameAs(manifest);
		manifest.getMainAttributes().clear();
		manifest.getEntries().clear();
		assertThat(copy.getMainAttributes()).isNotEmpty();
		assertThat(copy.getAttributes("spring")).isNotNull();
	}

	@Test
	void getEntryAttributesWhenSuppliedManifestIsNullReturnsNull() throws Exception {
		UrlJarManifest urlJarManifest = new UrlJarManifest(() -> null);
		assertThat(urlJarManifest.getEntryAttributes(new JarEntry("test"))).isNull();
	}

	@Test
	void getEntryAttributesReturnsDeepCopy() throws Exception {
		Manifest manifest = new Manifest();
		UrlJarManifest urlJarManifest = new UrlJarManifest(() -> manifest);
		Attributes attributes = new Attributes();
		attributes.putValue("test", "test");
		manifest.getEntries().put("spring", attributes);
		Attributes copy = urlJarManifest.getEntryAttributes(new JarEntry("spring"));
		assertThat(copy).isNotSameAs(attributes);
		attributes.clear();
		assertThat(copy.getValue("test")).isNotNull();

	}

	@Test
	void supplierIsOnlyCalledOnce() throws IOException {
		ManifestSupplier supplier = mock(ManifestSupplier.class);
		UrlJarManifest urlJarManifest = new UrlJarManifest(supplier);
		urlJarManifest.get();
		urlJarManifest.get();
		then(supplier).should(times(1)).getManifest();
	}

}
