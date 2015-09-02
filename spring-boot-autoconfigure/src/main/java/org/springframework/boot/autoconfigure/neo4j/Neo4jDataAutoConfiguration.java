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

import org.neo4j.ogm.session.Neo4jSession;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.neo4j.config.Neo4jConfiguration;
import org.springframework.data.neo4j.server.Neo4jServer;
import org.springframework.data.neo4j.template.Neo4jTemplate;

import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Collections;

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
@ConditionalOnClass({ Neo4jSession.class, Neo4jTemplate.class })
public abstract class Neo4jDataAutoConfiguration extends Neo4jConfiguration implements BeanClassLoaderAware, BeanFactoryAware {

	@Autowired
	private ResourceLoader resourceLoader;

	@Autowired
	private Neo4jProperties properties;

	@Autowired
	private Environment environment;

	private ClassLoader classLoader;
	private BeanFactory beanFactory;

	@Bean
	@Override
	@ConditionalOnMissingBean(Neo4jServer.class)
	public Neo4jServer neo4jServer()  {
		try {
			return this.properties.createNeo4jServer(this.environment);
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
	@ConditionalOnMissingBean(Neo4jTemplate.class)
	public Neo4jTemplate neo4jTemplate() throws Exception {
		return new Neo4jTemplate(getSession()); // todo converters
	}
/*
	@ConditionalOnMissingBean(Session.class)
	public Session neo4jDbFactory(Neo4jServer neo4j) throws Exception {
		return new Neo4jSession(neo4j);
	}

	@Bean
	@ConditionalOnMissingBean(Neo4jConverter.class)
	public MappingNeo4jConverter mappingNeo4jConverter(Neo4jFactory factory,
			Neo4jMappingContext context, BeanFactory beanFactory) {
		DbRefResolver dbRefResolver = new DefaultDbRefResolver(factory);
		MappingNeo4jConverter mappingConverter = new MappingNeo4jConverter(dbRefResolver,
				context);
		try {
			mappingConverter.setCustomConversions(beanFactory
					.getBean(CustomConversions.class));
		}
		catch (NoSuchBeanDefinitionException ex) {
			// Ignore
		}
		return mappingConverter;
	}

	@Bean
	@ConditionalOnMissingBean
	public Neo4jMappingContext neo4jMappingContext(BeanFactory beanFactory)
			throws ClassNotFoundException {
		Neo4jMappingContext context = new Neo4jMappingContext();
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
*/

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
