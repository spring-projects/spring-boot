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

package org.springframework.boot.autoconfigure.orm.jpa;

import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.cfg.ImprovedNamingStrategy;
import org.hibernate.ejb.HibernateEntityManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Hibernate JPA.
 * 
 * @author Phillip Webb
 */
@Configuration
@ConditionalOnClass(HibernateEntityManager.class)
@EnableTransactionManagement
public class HibernateJpaAutoConfiguration extends JpaBaseConfiguration {

	private static final Map<EmbeddedDatabaseType, String> EMBEDDED_DATABASE_DIALECTS;
	static {
		EMBEDDED_DATABASE_DIALECTS = new LinkedHashMap<EmbeddedDatabaseType, String>();
		EMBEDDED_DATABASE_DIALECTS.put(EmbeddedDatabaseType.HSQL,
				"org.hibernate.dialect.HSQLDialect");
	}

	@Value("${spring.jpa.databasePlatform:${spring.jpa.database_platform:}}")
	private String databasePlatform;

	@Value("${spring.jpa.database:DEFAULT}")
	private Database database = Database.DEFAULT;

	@Value("${spring.jpa.showSql:${spring.jpa.show_sql:true}}")
	private boolean showSql;

	@Value("${spring.jpa.ddlAuto:${spring.jpa.ddl_auto:none}}")
	private String ddlAuto; // e.g. none, validate, update, create, create-drop

	@Value("${spring.jpa.hibernate.namingstrategy:org.hibernate.cfg.ImprovedNamingStrategy}")
	private String namingStrategy;

	@Bean
	@Override
	public JpaVendorAdapter jpaVendorAdapter() {
		HibernateJpaVendorAdapter adapter = new HibernateJpaVendorAdapter();
		adapter.setShowSql(this.showSql);
		if (StringUtils.hasText(this.databasePlatform)) {
			adapter.setDatabasePlatform(this.databasePlatform);
		}
		adapter.setDatabase(this.database);
		return adapter;
	}

	@Override
	protected void configure(
			LocalContainerEntityManagerFactoryBean entityManagerFactoryBean) {
		Map<String, Object> properties = entityManagerFactoryBean.getJpaPropertyMap();
		properties.put("hibernate.cache.provider_class",
				"org.hibernate.cache.HashtableCacheProvider");
		if (StringUtils.hasLength(this.namingStrategy)) {
			properties.put("hibernate.ejb.naming_strategy", this.namingStrategy);
		}
		else {
			properties.put("hibernate.ejb.naming_strategy",
					ImprovedNamingStrategy.class.getName());
		}
		if (StringUtils.hasLength(this.ddlAuto) && !"none".equals(this.ddlAuto)) {
			properties.put("hibernate.hbm2ddl.auto", this.ddlAuto);
		}
	}

}
