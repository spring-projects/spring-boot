/*
 * Copyright 2012-2014 the original author or authors.
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

import java.util.Map;

import javax.persistence.EntityManager;
import javax.sql.DataSource;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.HibernateEntityManagerCondition;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.boot.orm.jpa.SpringNamingStrategy;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.AbstractJpaVendorAdapter;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.util.ClassUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Hibernate JPA.
 * 
 * @author Phillip Webb
 */
@Configuration
@ConditionalOnClass({ LocalContainerEntityManagerFactoryBean.class,
		EnableTransactionManagement.class, EntityManager.class })
@Conditional(HibernateEntityManagerCondition.class)
@ConditionalOnBean(DataSource.class)
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
public class HibernateJpaAutoConfiguration extends JpaBaseConfiguration implements
		BeanClassLoaderAware {

	private RelaxedPropertyResolver environment;

	private ClassLoader classLoader;

	public HibernateJpaAutoConfiguration() {
		this.environment = null;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	@Override
	public void setEnvironment(Environment environment) {
		super.setEnvironment(environment);
		this.environment = new RelaxedPropertyResolver(environment,
				"spring.jpa.hibernate.");
	}

	@Override
	protected AbstractJpaVendorAdapter createJpaVendorAdapter() {
		return new HibernateJpaVendorAdapter();
	}

	@Override
	protected void configure(
			LocalContainerEntityManagerFactoryBean entityManagerFactoryBean) {
		Map<String, Object> properties = entityManagerFactoryBean.getJpaPropertyMap();
		properties.put("hibernate.ejb.naming_strategy", this.environment.getProperty(
				"naming-strategy", SpringNamingStrategy.class.getName()));
		String ddlAuto = this.environment.getProperty("ddl-auto", getDefaultDdlAuto());
		if (!"none".equals(ddlAuto)) {
			properties.put("hibernate.hbm2ddl.auto", ddlAuto);
		}
	}

	private String getDefaultDdlAuto() {
		EmbeddedDatabaseConnection embeddedDatabaseConnection = EmbeddedDatabaseConnection
				.get(this.classLoader);
		if (embeddedDatabaseConnection == EmbeddedDatabaseConnection.NONE) {
			return "none";
		}
		return "create-drop";
	}

	static class HibernateEntityManagerCondition extends SpringBootCondition {

		private static String[] CLASS_NAMES = {
				"org.hibernate.ejb.HibernateEntityManager",
				"org.hibernate.jpa.HibernateEntityManager" };

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context,
				AnnotatedTypeMetadata metadata) {
			for (String className : CLASS_NAMES) {
				if (ClassUtils.isPresent(className, context.getClassLoader())) {
					return ConditionOutcome.match("found HibernateEntityManager class");
				}
			}
			return ConditionOutcome.noMatch("did not find HibernateEntityManager class");
		}
	}

}
