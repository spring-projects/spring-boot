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

package org.springframework.boot.autoconfigure.data.couchbase;

import com.couchbase.client.java.Bucket;
import org.junit.After;
import org.junit.Test;

import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.TestAutoConfigurationPackage;
import org.springframework.boot.autoconfigure.couchbase.CouchbaseProperties;
import org.springframework.boot.autoconfigure.couchbase.CouchbaseTestConfiguration;
import org.springframework.boot.autoconfigure.data.couchbase.city.City;
import org.springframework.boot.autoconfigure.data.couchbase.city.CityRepository;
import org.springframework.boot.autoconfigure.data.empty.EmptyDataPackage;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CouchbaseRepositoriesAutoConfiguration}.
 *
 * @author Eddú Meléndez
 */
public class CouchbaseRepositoriesAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void close() {
		this.context.close();
	}

	@Test
	public void testDefaultRepositoryConfiguration() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(TestConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean(CityRepository.class)).isNotNull();
		assertThat(this.context.getBean(Bucket.class)).isNotNull();
	}

	@Test
	public void testNoRepositoryConfiguration() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(EmptyConfiguration.class, TestConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean(Bucket.class)).isNotNull();
	}

	@Configuration
	@TestAutoConfigurationPackage(City.class)
	@EnableConfigurationProperties(CouchbaseProperties.class)
	@Import({ CouchbaseRepositoriesRegistrar.class, CouchbaseTestConfiguration.class })
	static class TestConfiguration {

	}

	@Configuration
	@TestAutoConfigurationPackage(EmptyDataPackage.class)
	protected static class EmptyConfiguration {

	}

}
