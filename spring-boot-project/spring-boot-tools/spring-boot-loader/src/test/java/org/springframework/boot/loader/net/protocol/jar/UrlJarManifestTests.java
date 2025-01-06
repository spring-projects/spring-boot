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
