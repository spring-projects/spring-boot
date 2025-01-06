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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Optimizations}.
 *
 * @author Phillip Webb
 */
class OptimizationsTests {

	@AfterEach
	void reset() {
		Optimizations.disable();
	}

	@Test
	void defaultIsNotEnabled() {
		assertThat(Optimizations.isEnabled()).isFalse();
		assertThat(Optimizations.isEnabled(true)).isFalse();
		assertThat(Optimizations.isEnabled(false)).isFalse();
	}

	@Test
	void enableWithReadContentsEnables() {
		Optimizations.enable(true);
		assertThat(Optimizations.isEnabled()).isTrue();
		assertThat(Optimizations.isEnabled(true)).isTrue();
		assertThat(Optimizations.isEnabled(false)).isFalse();
	}

	@Test
	void enableWithoutReadContentsEnables() {
		Optimizations.enable(false);
		assertThat(Optimizations.isEnabled()).isTrue();
		assertThat(Optimizations.isEnabled(true)).isFalse();
		assertThat(Optimizations.isEnabled(false)).isTrue();
	}

	@Test
	void enableIsByThread() throws InterruptedException {
		Optimizations.enable(true);
		boolean[] enabled = new boolean[1];
		Thread thread = new Thread(() -> enabled[0] = Optimizations.isEnabled());
		thread.start();
		thread.join();
		assertThat(enabled[0]).isFalse();
	}

	@Test
	void disableDisables() {
		Optimizations.enable(true);
		Optimizations.disable();
		assertThat(Optimizations.isEnabled()).isFalse();
		assertThat(Optimizations.isEnabled(true)).isFalse();
		assertThat(Optimizations.isEnabled(false)).isFalse();

	}

}
