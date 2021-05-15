/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.autoconfigure.session;

import java.time.Duration;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Tests for {@link SessionProperties}.
 *
 * @author Stephane Nicoll
 */
class SessionPropertiesTests {

	@Test
	@SuppressWarnings("unchecked")
	void determineTimeoutWithTimeoutIgnoreFallback() {
		SessionProperties properties = new SessionProperties();
		properties.setTimeout(Duration.ofMinutes(1));
		Supplier<Duration> fallback = mock(Supplier.class);
		assertThat(properties.determineTimeout(fallback)).isEqualTo(Duration.ofMinutes(1));
		verifyNoInteractions(fallback);
	}

	@Test
	void determineTimeoutWithNoTimeoutUseFallback() {
		SessionProperties properties = new SessionProperties();
		properties.setTimeout(null);
		Duration fallback = Duration.ofMinutes(2);
		assertThat(properties.determineTimeout(() -> fallback)).isSameAs(fallback);
	}

}
