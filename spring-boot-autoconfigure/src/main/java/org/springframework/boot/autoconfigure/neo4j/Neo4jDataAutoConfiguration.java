/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.autoconfigure.neo4j;

import java.util.Collection;
import java.util.Collections;

import org.neo4j.ogm.session.Neo4jSession;
import org.neo4j.ogm.session.SessionFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.core.env.Environment;

import org.springframework.data.neo4j.config.Neo4jConfiguration;
import org.springframework.data.neo4j.template.Neo4jOperations;
import org.springframework.data.neo4j.template.Neo4jTemplate;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data's Neo4j support.
 * <p>
 * Registers a {@link Neo4jTemplate} bean if no other bean of
 * the same type is configured.
 *
 * @author Michael Hunger
 * @author Josh Long
 * @author Vince Bickers
 * @since 1.3.0
 */
@Configuration
@EnableConfigurationProperties(Neo4jProperties.class)
@ConditionalOnMissingBean(type = "org.springframework.data.neo4j.template.Neo4jOperations")
@ConditionalOnClass({ Neo4jSession.class, Neo4jOperations.class })
public abstract class Neo4jDataAutoConfiguration extends Neo4jConfiguration implements BeanClassLoaderAware, BeanFactoryAware {

	@Autowired
	private Neo4jProperties properties;

	@Autowired
	private Environment environment;

	private ClassLoader classLoader;
	private BeanFactory beanFactory;

	@Bean
	@ConditionalOnMissingBean(org.neo4j.ogm.config.Configuration.class)
	public org.neo4j.ogm.config.Configuration configuration() {
		return this.properties.configure();
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	@Override
	@ConditionalOnMissingBean(SessionFactory.class)
	public SessionFactory getSessionFactory() {
		Collection<String> packages = getMappingBasePackages(this.beanFactory);
		return new SessionFactory(packages.toArray(new String[packages.size()]));
	}

	private static Collection<String> getMappingBasePackages(BeanFactory beanFactory) {
		try {
			return AutoConfigurationPackages.get(beanFactory);
		}
		catch (IllegalStateException ex) {
			// no auto-configuration package registered yet
			return Collections.emptyList();
		}
	}

}
