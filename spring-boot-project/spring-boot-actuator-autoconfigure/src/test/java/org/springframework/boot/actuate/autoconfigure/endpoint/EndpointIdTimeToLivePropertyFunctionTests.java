/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.autoconfigure.endpoint;

import java.util.function.Function;

import org.junit.Test;

import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EndpointIdTimeToLivePropertyFunction}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
public class EndpointIdTimeToLivePropertyFunctionTests {

	private final MockEnvironment environment = new MockEnvironment();

	private final Function<EndpointId, Long> timeToLive = new EndpointIdTimeToLivePropertyFunction(
			this.environment);

	@Test
	public void defaultConfiguration() {
		Long result = this.timeToLive.apply(EndpointId.of("test"));
		assertThat(result).isNull();
	}

	@Test
	public void userConfiguration() {
		this.environment.setProperty("management.endpoint.test.cache.time-to-live",
				"500");
		Long result = this.timeToLive.apply(EndpointId.of("test"));
		assertThat(result).isEqualTo(500L);
	}

	@Test
	public void mixedCaseUserConfiguration() {
		this.environment.setProperty(
				"management.endpoint.another-test.cache.time-to-live", "500");
		Long result = this.timeToLive.apply(EndpointId.of("anotherTest"));
		assertThat(result).isEqualTo(500L);
	}

}
