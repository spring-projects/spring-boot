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

import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.RelationshipEntity;
import org.neo4j.ogm.session.Neo4jSession;
import org.neo4j.ogm.session.SessionFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.neo4j.config.Neo4jConfiguration;
import org.springframework.data.neo4j.conversion.MetaDataDrivenConversionService;
import org.springframework.data.neo4j.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.server.Neo4jServer;
import org.springframework.data.neo4j.template.Neo4jTemplate;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data's Neo4j support.
 * <p>
 * Registers a {@link Neo4jTemplate} bean if no other bean of
 * the same type is configured.
 *
 * @author Michael Hunger
 * @author Josh Long
 * @since 1.3.0
 */
@Configuration
@EnableConfigurationProperties(Neo4jProperties.class)
@ConditionalOnMissingBean(type = "org.springframework.data.neo4j.template.Neo4jTemplate")
@ConditionalOnClass({ Neo4jServer.class, Neo4jSession.class, Neo4jTemplate.class })
//@ConditionalOnProperty(name = "", prefix = "spring.data.neo4j", matchIfMissing = true)
public class Neo4jDataAutoConfiguration extends Neo4jConfiguration implements BeanClassLoaderAware, BeanFactoryAware, EnvironmentAware, ResourceLoaderAware {

	@Autowired
	private Neo4jProperties properties;

	@Autowired
	private Environment environment;

	private ClassLoader classLoader;
	private BeanFactory beanFactory;
	private ResourceLoader resourceLoader;

	@Bean
	@Override
	@ConditionalOnMissingBean(Neo4jServer.class)
	public Neo4jServer neo4jServer()  {
		try {
			Neo4jProperties props = properties == null ?
					Neo4jProperties.fromEnvironment(environment) : this.properties;
			return props.createNeo4jServer(environment);
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
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
		Collection<String> packages = getMappingBasePackages(beanFactory);
		return new SessionFactory(packages.toArray(new String[packages.size()]));
	}

	@Bean
	public ConversionService conversionService(SessionFactory factory) {
		return new MetaDataDrivenConversionService(factory.metaData());
	}

	@Bean
//	@ConditionalOnMissingBean(Neo4jTemplate.class)
	public Neo4jTemplate neo4jTemplate() throws Exception {
		return new Neo4jTemplate(getSession()); // todo converters
	}


	@Bean
	@ConditionalOnMissingBean
	public Neo4jMappingContext neo4jMappingContext()
			throws ClassNotFoundException {
		Neo4jMappingContext context = new Neo4jMappingContext(getSessionFactory().metaData());
		context.setInitialEntitySet(getInitialEntitySet(beanFactory));
		return context;
	}

	private Set<Class<?>> getInitialEntitySet(BeanFactory beanFactory)
			throws ClassNotFoundException {
		Set<Class<?>> entitySet = new HashSet<Class<?>>();
		ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(
				false);
		scanner.setEnvironment(this.environment);
		scanner.setResourceLoader(this.resourceLoader);
		scanner.addIncludeFilter(new AnnotationTypeFilter(NodeEntity.class));
		scanner.addIncludeFilter(new AnnotationTypeFilter(RelationshipEntity.class));
		scanner.addIncludeFilter(new AnnotationTypeFilter(Persistent.class));
		for (String basePackage : getMappingBasePackages(beanFactory)) {
			if (StringUtils.hasText(basePackage)) {
				for (BeanDefinition candidate : scanner
						.findCandidateComponents(basePackage)) {
					entitySet.add(ClassUtils.forName(candidate.getBeanClassName(),
							this.classLoader));
				}
			}
		}
		return entitySet;
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

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}
}
