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

package org.springframework.autoconfigure.data;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.autoconfigure.data.JpaRepositoriesAutoConfiguration;
import org.springframework.autoconfigure.data.test.City;
import org.springframework.autoconfigure.data.test.CityRepository;
import org.springframework.autoconfigure.jdbc.EmbeddedDatabaseConfiguration;
import org.springframework.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.repository.support.DomainClassConverter;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import static org.junit.Assert.assertNotNull;

/**
 * @author Dave Syer
 */
@Ignore
// FIXME until spring data commons 1.6.0, jpa 1.5.0 available
public class JpaWebAutoConfigurationTests {

	private AnnotationConfigWebApplicationContext context;

	@Test
	public void testDefaultRepositoryConfiguration() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setServletContext(new MockServletContext());
		this.context.register(TestConfiguration.class,
				EmbeddedDatabaseConfiguration.class, HibernateJpaAutoConfiguration.class,
				JpaRepositoriesAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertNotNull(this.context.getBean(CityRepository.class));
		assertNotNull(this.context.getBean(DomainClassConverter.class));
	}

	@Configuration
	// @EnableSpringDataWebSupport
	@ComponentScan(basePackageClasses = City.class)
	protected static class TestConfiguration {

	}

}
