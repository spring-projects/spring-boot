/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.docs.jpa;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

/**
 * Example configuration for a custom JPA entity manager.
 *
 * @author Stephane Nicoll
 */
public class CustomEntityManagerFactoryExample {

	// tag::configuration[]
	@Bean
	@ConfigurationProperties("app.jpa.first")
	public JpaProperties firstJpaProperties() {
		return new JpaProperties();
	}

	@Bean
	public LocalContainerEntityManagerFactoryBean firstEntityManagerFactory(DataSource firstDataSource,
			JpaProperties firstJpaProperties) {
		EntityManagerFactoryBuilder builder = createEntityManagerFactoryBuilder(firstJpaProperties);
		return builder.dataSource(firstDataSource).packages(Order.class).persistenceUnit("firstDs").build();
	}

	private EntityManagerFactoryBuilder createEntityManagerFactoryBuilder(JpaProperties jpaProperties) {
		JpaVendorAdapter jpaVendorAdapter = createJpaVendorAdapter(jpaProperties);
		return new EntityManagerFactoryBuilder(jpaVendorAdapter, jpaProperties.getProperties(), null);
	}

	private JpaVendorAdapter createJpaVendorAdapter(JpaProperties jpaProperties) {
		// Map JPA properties as needed
		return new HibernateJpaVendorAdapter();
	}
	// end::configuration[]

	private static class Order {

	}

}
