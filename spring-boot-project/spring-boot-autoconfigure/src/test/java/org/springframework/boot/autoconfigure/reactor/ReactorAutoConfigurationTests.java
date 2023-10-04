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

package org.springframework.boot.autoconfigure.reactor;

import java.util.concurrent.atomic.AtomicReference;

import io.micrometer.context.ContextRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ReactorAutoConfiguration}.
 *
 * @author Brian Clozel
 */
class ReactorAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ReactorAutoConfiguration.class));

	private static final String THREADLOCAL_KEY = "ReactorAutoConfigurationTests";

	private static final ThreadLocal<String> THREADLOCAL_VALUE = ThreadLocal.withInitial(() -> "failure");

	@BeforeAll
	static void initializeThreadLocalAccessors() {
		ContextRegistry globalRegistry = ContextRegistry.getInstance();
		globalRegistry.registerThreadLocalAccessor(THREADLOCAL_KEY, THREADLOCAL_VALUE);
	}

	@Test
	void shouldConfigureAutomaticContextPropagation() {
		AtomicReference<String> threadLocalValue = new AtomicReference<>();
		this.contextRunner.run((applicationContext) -> {
			Mono.just("test")
				.doOnNext((element) -> threadLocalValue.set(THREADLOCAL_VALUE.get()))
				.contextWrite(Context.of(THREADLOCAL_KEY, "success"))
				.block();
			assertThat(threadLocalValue.get()).isEqualTo("success");
		});
	}

}
