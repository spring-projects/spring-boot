/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.docs.howto.dataaccess.usemultipleentitymanagers;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jpa.EntityManagerFactoryBuilder;
import org.springframework.boot.jpa.autoconfigure.JpaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

@Configuration(proxyBeanMethods = false)
public class MyAdditionalEntityManagerFactoryConfiguration {

	@Qualifier("second")
	@Bean(defaultCandidate = false)
	@ConfigurationProperties("app.jpa")
	public JpaProperties secondJpaProperties() {
		return new JpaProperties();
	}

	@Qualifier("second")
	@Bean(defaultCandidate = false)
	public LocalContainerEntityManagerFactoryBean secondEntityManagerFactory(@Qualifier("second") DataSource dataSource,
			@Qualifier("second") JpaProperties jpaProperties) {
		EntityManagerFactoryBuilder builder = createEntityManagerFactoryBuilder(jpaProperties);
		return builder.dataSource(dataSource).packages(Order.class).persistenceUnit("second").build();
	}

	private EntityManagerFactoryBuilder createEntityManagerFactoryBuilder(JpaProperties jpaProperties) {
		JpaVendorAdapter jpaVendorAdapter = createJpaVendorAdapter(jpaProperties);
		Function<DataSource, Map<String, ?>> jpaPropertiesFactory = (dataSource) -> createJpaProperties(dataSource,
				jpaProperties.getProperties());
		return new EntityManagerFactoryBuilder(jpaVendorAdapter, jpaPropertiesFactory, null);
	}

	private JpaVendorAdapter createJpaVendorAdapter(JpaProperties jpaProperties) {
		// ... map JPA properties as needed
		return new HibernateJpaVendorAdapter();
	}

	private Map<String, ?> createJpaProperties(DataSource dataSource, Map<String, ?> existingProperties) {
		Map<String, ?> jpaProperties = new LinkedHashMap<>(existingProperties);
		// ... map JPA properties that require the DataSource (e.g. DDL flags)
		return jpaProperties;
	}

}
