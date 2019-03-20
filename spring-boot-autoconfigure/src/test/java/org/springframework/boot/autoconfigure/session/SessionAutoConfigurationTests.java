/*
 * Copyright 2012-2017 the original author or authors.
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

import java.util.Arrays;
import java.util.Collections;

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
		this.thrown.expectMessage("No Spring Session store is configured");
		this.thrown.expectMessage("set the 'spring.session.store-type' property");
		load();
	}

	@Test
	public void contextFailsIfStoreTypeNotAvailable() {
		this.thrown.expect(BeanCreationException.class);
		this.thrown.expectMessage("No session repository could be auto-configured");
		this.thrown.expectMessage("session store type is 'mongo'");
		load("spring.session.store-type=mongo");
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
	public void mongoSessionStore() {
		load(Arrays.asList(EmbeddedMongoAutoConfiguration.class,
				MongoAutoConfiguration.class, MongoDataAutoConfiguration.class),
				"spring.session.store-type=mongo");
		validateSessionRepository(MongoOperationsSessionRepository.class);
	}

	@Test
	public void mongoSessionStoreWithCustomizations() {
		load(Arrays.asList(EmbeddedMongoAutoConfiguration.class,
				MongoAutoConfiguration.class, MongoDataAutoConfiguration.class),
				"spring.session.store-type=mongo",
				"spring.session.mongo.collection-name=foobar");
		MongoOperationsSessionRepository repository = validateSessionRepository(
				MongoOperationsSessionRepository.class);
		assertThat(new DirectFieldAccessor(repository).getPropertyValue("collectionName"))
				.isEqualTo("foobar");
	}

	@Configuration
	static class SessionRepositoryConfiguration {

		@Bean
		public SessionRepository<?> mySessionRepository() {
			return new MapSessionRepository(
					Collections.<String, ExpiringSession>emptyMap());
		}

	}

}
