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

package org.springframework.boot.ssl;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DefaultSslBundleRegistry}.
 *
 * @author Phillip Webb
 */
class DefaultSslBundleRegistryTests {

	private SslBundle bundle1 = mock(SslBundle.class);

	private SslBundle bundle2 = mock(SslBundle.class);

	private DefaultSslBundleRegistry registry = new DefaultSslBundleRegistry();

	@Test
	void createWithNameAndBundleRegistersBundle() {
		DefaultSslBundleRegistry registry = new DefaultSslBundleRegistry("test", this.bundle1);
		assertThat(registry.getBundle("test")).isSameAs(this.bundle1);
	}

	@Test
	void registerBundleWhenNameIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.registry.registerBundle(null, this.bundle1))
			.withMessage("Name must not be null");
	}

	@Test
	void registerBundleWhenBundleIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.registry.registerBundle("test", null))
			.withMessage("Bundle must not be null");
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
			.withMessage("Name must not be null");
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

}
