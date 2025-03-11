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

package org.springframework.boot.http.client;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test customizer that can assert that it has been called.
 *
 * @param <T> type being customized
 * @author Phillip Webb
 */
class TestCustomizer<T> implements Consumer<T> {

	private boolean called;

	@Override
	public void accept(T t) {
		this.called = true;
	}

	void assertCalled() {
		assertThat(this.called).isTrue();
	}

}
