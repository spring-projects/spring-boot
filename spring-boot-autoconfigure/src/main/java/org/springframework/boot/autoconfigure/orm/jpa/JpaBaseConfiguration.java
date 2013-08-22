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

import java.util.List;

import javax.persistence.EntityManager;
import javax.sql.DataSource;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurationUtils;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewFilter;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewInterceptor;
import org.springframework.orm.jpa.vendor.AbstractJpaVendorAdapter;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.util.Assert;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * Base {@link EnableAutoConfiguration Auto-configuration} for JPA.
 * 
 * @author Phillip Webb
 * @author Dave Syer
 */
@ConditionalOnClass({ LocalContainerEntityManagerFactoryBean.class,
		EnableTransactionManagement.class, EntityManager.class })
@ConditionalOnBean(DataSource.class)
public abstract class JpaBaseConfiguration implements BeanFactoryAware, EnvironmentAware {

	private ConfigurableListableBeanFactory beanFactory;

	private RelaxedPropertyResolver environment;

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = new RelaxedPropertyResolver(environment, "spring.jpa.");
	}

	@Bean
	public PlatformTransactionManager transactionManager() {
		return new JpaTransactionManager(entityManagerFactory().getObject());
	}

	@Bean
	public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
		LocalContainerEntityManagerFactoryBean entityManagerFactoryBean = new LocalContainerEntityManagerFactoryBean();
		entityManagerFactoryBean.setJpaVendorAdapter(jpaVendorAdapter());
		entityManagerFactoryBean.setDataSource(getDataSource());
		entityManagerFactoryBean.setPackagesToScan(getPackagesToScan());
		entityManagerFactoryBean.getJpaPropertyMap().putAll(
				this.environment.getSubProperties("properties."));
		configure(entityManagerFactoryBean);
		return entityManagerFactoryBean;
	}

	@Bean
	public JpaVendorAdapter jpaVendorAdapter() {
		AbstractJpaVendorAdapter adapter = createJpaVendorAdapter();
		adapter.setShowSql(this.environment.getProperty("show-sql", Boolean.class, true));
		adapter.setDatabasePlatform(this.environment.getProperty("database-platform"));
		adapter.setDatabase(this.environment.getProperty("database", Database.class,
				Database.DEFAULT));
		adapter.setGenerateDdl(this.environment.getProperty("generate-ddl",
				Boolean.class, false));
		return adapter;
	}

	protected abstract AbstractJpaVendorAdapter createJpaVendorAdapter();

	protected DataSource getDataSource() {
		try {
			return this.beanFactory.getBean("dataSource", DataSource.class);
		}
		catch (RuntimeException ex) {
			return this.beanFactory.getBean(DataSource.class);
		}
	}

	protected String[] getPackagesToScan() {
		List<String> basePackages = AutoConfigurationUtils
				.getBasePackages(this.beanFactory);
		Assert.notEmpty(basePackages,
				"Unable to find JPA packages to scan, please define "
						+ "a @ComponentScan annotation or disable JpaAutoConfiguration");
		return basePackages.toArray(new String[basePackages.size()]);
	}

	protected void configure(
			LocalContainerEntityManagerFactoryBean entityManagerFactoryBean) {
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
	}

	@Configuration
	@ConditionalOnWebApplication
	@ConditionalOnMissingBean({ OpenEntityManagerInViewInterceptor.class,
			OpenEntityManagerInViewFilter.class })
	@ConditionalOnExpression("${spring.jpa.openInView:${spring.jpa.open_in_view:true}}")
	protected static class JpaWebConfiguration extends WebMvcConfigurerAdapter {

		@Override
		public void addInterceptors(InterceptorRegistry registry) {
			registry.addWebRequestInterceptor(openEntityManagerInViewInterceptor());
		}

		@Bean
		public OpenEntityManagerInViewInterceptor openEntityManagerInViewInterceptor() {
			return new OpenEntityManagerInViewInterceptor();
		}

	}
}
