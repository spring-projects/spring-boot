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

package org.springframework.boot.autoconfigure.session;

import java.util.Arrays;
import java.util.Collections;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.ExpiringSession;
import org.springframework.session.MapSessionRepository;
import org.springframework.session.SessionRepository;
import org.springframework.session.data.mongo.MongoOperationsSessionRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link SessionAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 */
public class SessionAutoConfigurationTests extends AbstractSessionAutoConfigurationTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void contextFailsIfStoreTypeNotSet() {
		this.thrown.expect(BeanCreationException.class);
		this.thrown.expectMessage("No session repository could be auto-configured");
		this.thrown.expectMessage("session store type is 'null'");
		load();
	}

	@Test
	public void autoConfigurationDisabledIfStoreTypeSetToNone() {
		load("spring.session.store-type=none");
		assertThat(this.context.getBeansOfType(SessionRepository.class)).hasSize(0);
	}

	@Test
	public void backOffIfSessionRepositoryIsPresent() {
		load(Collections.<Class<?>>singletonList(SessionRepositoryConfiguration.class),
				"spring.session.store-type=mongo");
		MapSessionRepository repository = validateSessionRepository(
				MapSessionRepository.class);
		assertThat(this.context.getBean("mySessionRepository")).isSameAs(repository);
	}

	@Test
	public void hashMapSessionStore() {
		load("spring.session.store-type=hash-map");
		MapSessionRepository repository = validateSessionRepository(
				MapSessionRepository.class);
		assertThat(getSessionTimeout(repository)).isNull();
	}

	@Test
	public void hashMapSessionStoreCustomTimeout() {
		load("spring.session.store-type=hash-map", "server.session.timeout=3000");
		MapSessionRepository repository = validateSessionRepository(
				MapSessionRepository.class);
		assertThat(getSessionTimeout(repository)).isEqualTo(3000);
	}

	@Test
	public void springSessionTimeoutIsNotAValidProperty() {
		load("spring.session.store-type=hash-map", "spring.session.timeout=3000");
		MapSessionRepository repository = validateSessionRepository(
				MapSessionRepository.class);
		assertThat(getSessionTimeout(repository)).isNull();
	}

	@Test
	public void hazelcastSessionStore() {
		load(Collections.<Class<?>>singletonList(HazelcastConfiguration.class),
				"spring.session.store-type=hazelcast");
		validateSessionRepository(MapSessionRepository.class);
	}

	@Test
	public void hazelcastSessionStoreWithCustomizations() {
		load(Collections.<Class<?>>singletonList(HazelcastSpecificMap.class),
				"spring.session.store-type=hazelcast",
				"spring.session.hazelcast.map-name=foo:bar:biz");
		validateSessionRepository(MapSessionRepository.class);
		HazelcastInstance hazelcastInstance = this.context
				.getBean(HazelcastInstance.class);
		verify(hazelcastInstance, times(1)).getMap("foo:bar:biz");
	}

	@Test
	public void mongoSessionStore() {
		load(Arrays.asList(EmbeddedMongoAutoConfiguration.class,
				MongoAutoConfiguration.class, MongoDataAutoConfiguration.class),
				"spring.session.store-type=mongo", "spring.data.mongodb.port=0");
		validateSessionRepository(MongoOperationsSessionRepository.class);
	}

	@Test
	public void mongoSessionStoreWithCustomizations() {
		load(Arrays.asList(EmbeddedMongoAutoConfiguration.class,
				MongoAutoConfiguration.class, MongoDataAutoConfiguration.class),
				"spring.session.store-type=mongo", "spring.data.mongodb.port=0",
				"spring.session.mongo.collection-name=foobar");
		MongoOperationsSessionRepository repository = validateSessionRepository(
				MongoOperationsSessionRepository.class);
		assertThat(new DirectFieldAccessor(repository).getPropertyValue("collectionName"))
				.isEqualTo("foobar");
	}

	@Test
	public void validationFailsIfSessionRepositoryIsNotConfigured() {
		this.thrown.expect(BeanCreationException.class);
		this.thrown.expectMessage("No session repository could be auto-configured");
		this.thrown.expectMessage("session store type is 'JDBC'");
		load("spring.session.store-type=jdbc");
	}

	@Configuration
	static class SessionRepositoryConfiguration {

		@Bean
		public SessionRepository<?> mySessionRepository() {
			return new MapSessionRepository(
					Collections.<String, ExpiringSession>emptyMap());
		}

	}

	@Configuration
	static class HazelcastConfiguration {

		@Bean
		public HazelcastInstance hazelcastInstance() {
			return Hazelcast.newHazelcastInstance();
		}

	}

	@Configuration
	static class HazelcastSpecificMap {

		@Bean
		@SuppressWarnings("unchecked")
		public HazelcastInstance hazelcastInstance() {
			IMap<Object, Object> map = mock(IMap.class);
			HazelcastInstance mock = mock(HazelcastInstance.class);
			given(mock.getMap("foo:bar:biz")).willReturn(map);
			return mock;
		}

	}

}
