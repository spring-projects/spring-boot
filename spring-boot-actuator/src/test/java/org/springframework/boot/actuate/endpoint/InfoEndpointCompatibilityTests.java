/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.actuate.endpoint;

import java.util.Collections;
import java.util.Map;

import org.junit.Test;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link InfoEndpoint}.
 *
 * @author Dave Syer
 */
@Deprecated
public class InfoEndpointCompatibilityTests {

	@Test
	public void invoke() throws Exception {
		Map<String, Object> actual = getEndpointBean().invoke();
		assertThat(actual.get("key1")).isEqualTo("value1");
		assertThat(actual.get("foo")).isEqualTo("bar");
	}

	private InfoEndpoint getEndpointBean() {
		return new InfoEndpoint(Collections.<String, Object>singletonMap("foo", "bar"),
				infoContributor());
	}

	private InfoContributor infoContributor() {
		return new InfoContributor() {

			@Override
			public void contribute(Info.Builder builder) {
				builder.withDetail("key1", "value1");
			}

		};
	}

}
