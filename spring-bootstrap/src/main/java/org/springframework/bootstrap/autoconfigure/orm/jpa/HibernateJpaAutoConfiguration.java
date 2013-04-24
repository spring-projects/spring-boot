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

package org.springframework.bootstrap.autoconfigure.orm.jpa;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.bootstrap.autoconfigure.jdbc.EmbeddedDatabaseAutoConfiguration;
import org.springframework.bootstrap.context.annotation.ConditionalOnClass;
import org.springframework.bootstrap.context.annotation.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Hibernate JPA.
 * 
 * @author Phillip Webb
 */
@Configuration
@ConditionalOnClass(name = "org.hibernate.ejb.HibernateEntityManager")
@EnableTransactionManagement
public class HibernateJpaAutoConfiguration extends JpaAutoConfiguration {

	private static final Map<EmbeddedDatabaseType, String> EMBEDDED_DATABASE_DIALECTS;
	static {
		EMBEDDED_DATABASE_DIALECTS = new LinkedHashMap<EmbeddedDatabaseType, String>();
		EMBEDDED_DATABASE_DIALECTS.put(EmbeddedDatabaseType.HSQL,
				"org.hibernate.dialect.HSQLDialect");
	}

	@Bean
	@Override
	public JpaVendorAdapter jpaVendorAdapter() {
		return new HibernateJpaVendorAdapter();
	}

	@Override
	protected void configure(
			LocalContainerEntityManagerFactoryBean entityManagerFactoryBean) {
		Map<String, Object> properties = entityManagerFactoryBean.getJpaPropertyMap();
		if (isAutoConfiguredDataSource()) {
			properties.put("hibernate.hbm2ddl.auto", "create-drop");
			String dialect = EMBEDDED_DATABASE_DIALECTS
					.get(EmbeddedDatabaseAutoConfiguration.getEmbeddedDatabaseType());
			if (dialect != null) {
				properties.put("hibernate.dialect", dialect);
			}
		}
		properties.put("hibernate.cache.provider_class",
				"org.hibernate.cache.HashtableCacheProvider");
	}

}
