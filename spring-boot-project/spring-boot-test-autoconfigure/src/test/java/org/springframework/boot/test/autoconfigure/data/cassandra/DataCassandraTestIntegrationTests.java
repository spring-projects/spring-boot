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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Sample test for {@link DataCassandraTest @DataCassandraTest} .
 *
 * @author Dmytro Nosan
 */
@RunWith(SpringRunner.class)
@DataCassandraTest
public class DataCassandraTestIntegrationTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Autowired
	private CassandraTemplate cassandraTemplate;

	@Autowired
	private ExampleRepository exampleRepository;

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	public void testRepository() {
		City city = new City();
		city.setId(Long.MAX_VALUE);
		city.setName("Lviv");
		city = this.exampleRepository.save(city);
		assertThat(city.getId()).isNotNull();
		assertThat(this.cassandraTemplate.exists(city.getId(), City.class))
				.isTrue();
	}

	@Test
	public void didNotInjectExampleService() {
		this.thrown.expect(NoSuchBeanDefinitionException.class);
		this.applicationContext.getBean(ExampleService.class);
	}



}
