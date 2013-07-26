/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.config.data;

import javax.persistence.EntityManagerFactory;

import org.junit.Test;
import org.springframework.boot.config.ComponentScanDetectorConfiguration;
import org.springframework.boot.config.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.config.data.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.config.data.test.City;
import org.springframework.boot.config.data.test.CityRepository;
import org.springframework.boot.config.jdbc.EmbeddedDatabaseConfiguration;
import org.springframework.boot.config.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import static org.junit.Assert.assertNotNull;

/**
 * Tests for {@link JpaRepositoriesAutoConfiguration}.
 * 
 * @author Dave Syer
 */
public class JpaRepositoriesAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@Test
	public void testDefaultRepositoryConfiguration() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(TestConfiguration.class,
				ComponentScanDetectorConfiguration.class,
				EmbeddedDatabaseConfiguration.class, HibernateJpaAutoConfiguration.class,
				JpaRepositoriesAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertNotNull(this.context.getBean(CityRepository.class));
		assertNotNull(this.context.getBean(PlatformTransactionManager.class));
		assertNotNull(this.context.getBean(EntityManagerFactory.class));
	}

	@Configuration
	@ComponentScan(basePackageClasses = City.class)
	protected static class TestConfiguration {

	}

}
