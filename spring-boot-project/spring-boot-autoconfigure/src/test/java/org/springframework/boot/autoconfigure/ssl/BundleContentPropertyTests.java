/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.autoconfigure.ssl;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import org.springframework.boot.io.ApplicationResourceLoader;
import org.springframework.core.io.ResourceLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.spy;

/**
 * Tests for {@link BundleContentProperty}.
 *
 * @author Moritz Halbritter
 * @author Phillip Webb
 */
class BundleContentPropertyTests {

	private static final String PEM_TEXT = """
			-----BEGIN CERTIFICATE-----
			-----END CERTIFICATE-----
			""";

	@Test
	void isPemContentWhenValueIsPemTextReturnsTrue() {
		BundleContentProperty property = new BundleContentProperty("name", PEM_TEXT);
		assertThat(property.isPemContent()).isTrue();
	}

	@Test
	void isPemContentWhenValueIsNotPemTextReturnsFalse() {
		BundleContentProperty property = new BundleContentProperty("name", "file.pem");
		assertThat(property.isPemContent()).isFalse();
	}

	@Test
	void hasValueWhenHasValueReturnsTrue() {
		BundleContentProperty property = new BundleContentProperty("name", "file.pem");
		assertThat(property.hasValue()).isTrue();
	}

	@Test
	void hasValueWhenHasNullValueReturnsFalse() {
		BundleContentProperty property = new BundleContentProperty("name", null);
		assertThat(property.hasValue()).isFalse();
	}

	@Test
	void hasValueWhenHasEmptyValueReturnsFalse() {
		BundleContentProperty property = new BundleContentProperty("name", "");
		assertThat(property.hasValue()).isFalse();
	}

	@Test
	void toWatchPathWhenNotPathThrowsException() {
		BundleContentProperty property = new BundleContentProperty("name", PEM_TEXT);
		assertThatIllegalStateException().isThrownBy(() -> property.toWatchPath(ApplicationResourceLoader.get()))
			.withMessage("Unable to convert value of property 'name' to a path");
	}

	@Test
	void toWatchPathWhenPathReturnsPath() throws URISyntaxException {
		URL resource = getClass().getResource("keystore.jks");
		Path file = Path.of(resource.toURI()).toAbsolutePath();
		BundleContentProperty property = new BundleContentProperty("name", file.toString());
		assertThat(property.toWatchPath(ApplicationResourceLoader.get())).isEqualTo(file);
	}

	@Test
	void toWatchPathUsesResourceLoader() throws URISyntaxException {
		URL resource = getClass().getResource("keystore.jks");
		Path file = Path.of(resource.toURI()).toAbsolutePath();
		BundleContentProperty property = new BundleContentProperty("name", file.toString());
		ResourceLoader resourceLoader = spy(ApplicationResourceLoader.get());
		assertThat(property.toWatchPath(resourceLoader)).isEqualTo(file);
		then(resourceLoader).should(atLeastOnce()).getResource(file.toString());
	}

	@Test
	void shouldThrowBundleContentNotWatchableExceptionIfContentIsNotWatchable() {
		BundleContentProperty property = new BundleContentProperty("name", "https://example.com/");
		assertThatExceptionOfType(BundleContentNotWatchableException.class)
			.isThrownBy(() -> property.toWatchPath(ApplicationResourceLoader.get()))
			.withMessageContaining("Only 'file:' resources are watchable");
	}

}
