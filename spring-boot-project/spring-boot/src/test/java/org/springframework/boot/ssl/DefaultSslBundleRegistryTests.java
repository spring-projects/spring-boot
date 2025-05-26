/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.ssl;

import java.util.concurrent.atomic.AtomicReference;

import org.awaitility.Awaitility;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.testsupport.system.CapturedOutput;
import org.springframework.boot.testsupport.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DefaultSslBundleRegistry}.
 *
 * @author Phillip Webb
 * @author Moritz Halbritter
 */
@ExtendWith(OutputCaptureExtension.class)
class DefaultSslBundleRegistryTests {

	private final SslBundle bundle1 = mock(SslBundle.class);

	private final SslBundle bundle2 = mock(SslBundle.class);

	private DefaultSslBundleRegistry registry;

	@BeforeEach
	void setUp() {
		this.registry = new DefaultSslBundleRegistry();
	}

	@Test
	void createWithNameAndBundleRegistersBundle() {
		DefaultSslBundleRegistry registry = new DefaultSslBundleRegistry("test", this.bundle1);
		assertThat(registry.getBundle("test")).isSameAs(this.bundle1);
	}

	@Test
	void registerBundleWhenNameIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.registry.registerBundle(null, this.bundle1))
			.withMessage("'name' must not be null");
	}

	@Test
	void registerBundleWhenBundleIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.registry.registerBundle("test", null))
			.withMessage("'bundle' must not be null");
	}

	@Test
	void registerBundleWhenNameIsTakenThrowsException() {
		this.registry.registerBundle("test", this.bundle1);
		assertThatIllegalStateException().isThrownBy(() -> this.registry.registerBundle("test", this.bundle2))
			.withMessage("Cannot replace existing SSL bundle 'test'");
	}

	@Test
	void registerBundleRegistersBundle() {
		this.registry.registerBundle("test", this.bundle1);
		assertThat(this.registry.getBundle("test")).isSameAs(this.bundle1);
	}

	@Test
	void getBundleWhenNameIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.registry.getBundle(null))
			.withMessage("'name' must not be null");
	}

	@Test
	void getBundleWhenNoSuchBundleThrowsException() {
		this.registry.registerBundle("test", this.bundle1);
		assertThatExceptionOfType(NoSuchSslBundleException.class).isThrownBy(() -> this.registry.getBundle("missing"))
			.satisfies((ex) -> assertThat(ex.getBundleName()).isEqualTo("missing"));
	}

	@Test
	void getBundleReturnsBundle() {
		this.registry.registerBundle("test1", this.bundle1);
		this.registry.registerBundle("test2", this.bundle2);
		assertThat(this.registry.getBundle("test1")).isSameAs(this.bundle1);
		assertThat(this.registry.getBundle("test2")).isSameAs(this.bundle2);
	}

	@Test
	void getBundleNamesReturnsNames() {
		this.registry.registerBundle("test1", this.bundle1);
		this.registry.registerBundle("test2", this.bundle2);
		assertThat(this.registry.getBundleNames()).containsExactly("test1", "test2");
	}

	@Test
	void updateBundleShouldNotifyUpdateHandlers() {
		AtomicReference<SslBundle> updatedBundle = new AtomicReference<>();
		this.registry.registerBundle("test1", this.bundle1);
		this.registry.addBundleUpdateHandler("test1", updatedBundle::set);
		this.registry.updateBundle("test1", this.bundle2);
		Awaitility.await().untilAtomic(updatedBundle, Matchers.equalTo(this.bundle2));
	}

	@Test
	void shouldFailIfUpdatingNonRegisteredBundle() {
		assertThatExceptionOfType(NoSuchSslBundleException.class)
			.isThrownBy(() -> this.registry.updateBundle("dummy", this.bundle1))
			.withMessageContaining("'dummy'");
	}

	@Test
	void shouldLogIfUpdatingBundleWithoutListeners(CapturedOutput output) {
		this.registry.registerBundle("test1", this.bundle1);
		this.registry.getBundle("test1");
		this.registry.updateBundle("test1", this.bundle2);
		assertThat(output).contains(
				"SSL bundle 'test1' has been updated but may be in use by a technology that doesn't support SSL reloading");
	}

}
