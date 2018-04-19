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

package org.springframework.boot.test.autoconfigure.data.cassandra;

import java.time.Duration;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testsupport.testcontainers.CassandraContainer;
import org.springframework.data.cassandra.core.ReactiveCassandraTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sample tests for {@link DataCassandraTest} using reactive repositories.
 *
 * @author Dmytro Nosan
 */
@RunWith(SpringRunner.class)
@DataCassandraTest
@ContextConfiguration(initializers = DataCassandraTestReactiveIntegrationTests.Initializer.class)
public class DataCassandraTestReactiveIntegrationTests {

	@ClassRule
	public static CassandraContainer cassandra = new CassandraContainer();

	@Autowired
	private ReactiveCassandraTemplate cassandraTemplate;

	@Autowired
	private ExampleReactiveRepository exampleRepository;

	@Test
	public void testRepository() {

		City city = new City();
		city.setId(Long.MAX_VALUE % 3);
		city.setName("Lviv");
		city = this.exampleRepository.save(city).block(Duration.ofMinutes(1));
		assertThat(city.getId()).isNotNull();
		assertThat(this.cassandraTemplate.exists(city.getId(), City.class).block(Duration.ofMinutes(1)))
				.isTrue();
	}


	static class Initializer extends AbstractCassandraInitializer {

		@Override
		int getMappedPort() {
			return cassandra.getMappedPort();
		}
	}


}
