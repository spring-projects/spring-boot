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

package org.springframework.boot.autoconfigure.liquibase;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import liquibase.integration.spring.SpringLiquibase;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.orm.jpa.AbstractEntityManagerFactoryBean;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import static java.util.Arrays.asList;
import static org.springframework.beans.factory.BeanFactoryUtils.beanNamesForTypeIncludingAncestors;
import static org.springframework.beans.factory.BeanFactoryUtils.transformedBeanName;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Liquibase.
 * 
 * @author Marcel Overdijk
 * @author Dave Syer
 * @since 1.1.0
 */
@Configuration
@ConditionalOnClass(SpringLiquibase.class)
@ConditionalOnBean(DataSource.class)
@ConditionalOnExpression("${liquibase.enabled:true}")
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
public class LiquibaseAutoConfiguration {

	@Configuration
	@ConditionalOnMissingBean(SpringLiquibase.class)
	@EnableConfigurationProperties(LiquibaseProperties.class)
	@Import(LiquibaseJpaDependencyConfiguration.class)
	public static class LiquibaseConfiguration {

		@Autowired
		private LiquibaseProperties properties = new LiquibaseProperties();

		@Autowired
		private ResourceLoader resourceLoader = new DefaultResourceLoader();

		@Autowired
		private DataSource dataSource;

		@PostConstruct
		public void checkChangelogExists() {
			if (this.properties.isCheckChangeLogLocation()) {
				Resource resource = this.resourceLoader.getResource(this.properties
						.getChangeLog());
				Assert.state(resource.exists(), "Cannot find changelog location: "
						+ resource
						+ " (please add changelog or check your Liquibase configuration)");
			}
		}

		@Bean
		public SpringLiquibase liquibase() {
			SpringLiquibase liquibase = new SpringLiquibase();
			liquibase.setChangeLog(this.properties.getChangeLog());
			liquibase.setContexts(this.properties.getContexts());
			liquibase.setDataSource(this.dataSource);
			liquibase.setDefaultSchema(this.properties.getDefaultSchema());
			liquibase.setDropFirst(this.properties.isDropFirst());
			liquibase.setShouldRun(this.properties.isEnabled());
			return liquibase;
		}
	}

	@Configuration
	@ConditionalOnClass(LocalContainerEntityManagerFactoryBean.class)
	@ConditionalOnBean(AbstractEntityManagerFactoryBean.class)
	protected static class LiquibaseJpaDependencyConfiguration implements
			BeanFactoryPostProcessor {

		public static final String LIQUIBASE_JPA_BEAN_NAME = "liquibase";

		@Override
		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {

			for (String beanName : getEntityManagerFactoryBeanNames(beanFactory)) {
				BeanDefinition definition = getBeanDefinition(beanName, beanFactory);
				definition.setDependsOn(StringUtils.addStringToArray(
						definition.getDependsOn(), LIQUIBASE_JPA_BEAN_NAME));
			}
		}

		private static BeanDefinition getBeanDefinition(String beanName,
				ConfigurableListableBeanFactory beanFactory) {
			try {
				return beanFactory.getBeanDefinition(beanName);
			}
			catch (NoSuchBeanDefinitionException e) {

				BeanFactory parentBeanFactory = beanFactory.getParentBeanFactory();

				if (parentBeanFactory instanceof ConfigurableListableBeanFactory) {
					return getBeanDefinition(beanName,
							(ConfigurableListableBeanFactory) parentBeanFactory);
				}

				throw e;
			}
		}

		private static Iterable<String> getEntityManagerFactoryBeanNames(
				ListableBeanFactory beanFactory) {

			Set<String> names = new HashSet<String>();
			names.addAll(asList(beanNamesForTypeIncludingAncestors(beanFactory,
					EntityManagerFactory.class, true, false)));

			for (String factoryBeanName : beanNamesForTypeIncludingAncestors(beanFactory,
					AbstractEntityManagerFactoryBean.class, true, false)) {
				names.add(transformedBeanName(factoryBeanName));
			}

			return names;
		}

	}

}
