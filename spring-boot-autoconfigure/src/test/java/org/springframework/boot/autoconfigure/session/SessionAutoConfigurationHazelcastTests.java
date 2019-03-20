/*
 * Copyright 2012-2016 the original author or authors.
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

import java.util.Collections;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.hazelcast.HazelcastFlushMode;
import org.springframework.session.hazelcast.HazelcastSessionRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Hazelcast specific tests for {@link SessionAutoConfiguration}.
 *
 * @author Vedran Pavic
 */
public class SessionAutoConfigurationHazelcastTests
		extends AbstractSessionAutoConfigurationTests {

	@Test
	public void defaultConfig() {
		load(Collections.<Class<?>>singletonList(HazelcastConfiguration.class),
				"spring.session.store-type=hazelcast");
		validateSessionRepository(HazelcastSessionRepository.class);
		HazelcastInstance hazelcastInstance = this.context
				.getBean(HazelcastInstance.class);
		verify(hazelcastInstance, times(1)).getMap("spring:session:sessions");
	}

	@Test
	public void customMapName() {
		load(Collections.<Class<?>>singletonList(HazelcastConfiguration.class),
				"spring.session.store-type=hazelcast",
				"spring.session.hazelcast.map-name=foo:bar:biz");
		validateSessionRepository(HazelcastSessionRepository.class);
		HazelcastInstance hazelcastInstance = this.context
				.getBean(HazelcastInstance.class);
		verify(hazelcastInstance, times(1)).getMap("foo:bar:biz");
	}

	@Test
	public void customFlushMode() {
		load(Collections.<Class<?>>singletonList(HazelcastConfiguration.class),
				"spring.session.store-type=hazelcast",
				"spring.session.hazelcast.flush-mode=immediate");
		HazelcastSessionRepository repository = validateSessionRepository(
				HazelcastSessionRepository.class);
		assertThat(new DirectFieldAccessor(repository)
				.getPropertyValue("hazelcastFlushMode"))
						.isEqualTo(HazelcastFlushMode.IMMEDIATE);
	}

	@Configuration
	static class HazelcastConfiguration {

		@Bean
		@SuppressWarnings("unchecked")
		public HazelcastInstance hazelcastInstance() {
			IMap<Object, Object> map = mock(IMap.class);
			HazelcastInstance mock = mock(HazelcastInstance.class);
			given(mock.getMap("spring:session:sessions")).willReturn(map);
			given(mock.getMap("foo:bar:biz")).willReturn(map);
			return mock;
		}

	}

}
