/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.jmx;

import javax.management.MBeanServer;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableMBeanExport;
import org.springframework.context.annotation.Primary;
import org.springframework.jmx.export.MBeanExporter;
import org.springframework.jmx.export.annotation.AnnotationJmxAttributeSource;
import org.springframework.jmx.export.annotation.AnnotationMBeanExporter;
import org.springframework.jmx.export.naming.ObjectNamingStrategy;
import org.springframework.jmx.support.MBeanServerFactoryBean;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} to enable/disable Spring's
 * {@link EnableMBeanExport @EnableMBeanExport} mechanism based on configuration
 * properties.
 * <p>
 * To enable auto export of annotation beans set {@code spring.jmx.enabled: true}.
 *
 * @author Christian Dupuis
 * @author Madhura Bhave
 * @author Artsiom Yudovin
 * @author Scott Frederick
 * @since 1.0.0
 */
@AutoConfiguration
@EnableConfigurationProperties(JmxProperties.class)
@ConditionalOnClass({ MBeanExporter.class })
@ConditionalOnProperty(prefix = "spring.jmx", name = "enabled", havingValue = "true")
public class JmxAutoConfiguration {

	private final JmxProperties properties;

	/**
	 * Constructs a new JmxAutoConfiguration with the specified JmxProperties.
	 * @param properties the JmxProperties to be used for configuration
	 */
	public JmxAutoConfiguration(JmxProperties properties) {
		this.properties = properties;
	}

	/**
	 * Creates an instance of {@link AnnotationMBeanExporter} if no other bean of type
	 * {@link MBeanExporter} is present in the application context. The created exporter
	 * is configured with the provided {@link ObjectNamingStrategy} and
	 * {@link BeanFactory}. The exporter's registration policy is set based on the
	 * configuration properties. The exporter's naming strategy is set to the provided
	 * naming strategy. If a server bean name is specified in the configuration
	 * properties, the exporter's server is set to the MBeanServer bean with that name.
	 * The exporter's ensure unique runtime object names flag is set based on the
	 * configuration properties.
	 * @param namingStrategy the object naming strategy to be used by the exporter
	 * @param beanFactory the bean factory to be used by the exporter
	 * @return the created {@link AnnotationMBeanExporter} instance
	 */
	@Bean
	@Primary
	@ConditionalOnMissingBean(value = MBeanExporter.class, search = SearchStrategy.CURRENT)
	public AnnotationMBeanExporter mbeanExporter(ObjectNamingStrategy namingStrategy, BeanFactory beanFactory) {
		AnnotationMBeanExporter exporter = new AnnotationMBeanExporter();
		exporter.setRegistrationPolicy(this.properties.getRegistrationPolicy());
		exporter.setNamingStrategy(namingStrategy);
		String serverBean = this.properties.getServer();
		if (StringUtils.hasLength(serverBean)) {
			exporter.setServer(beanFactory.getBean(serverBean, MBeanServer.class));
		}
		exporter.setEnsureUniqueRuntimeObjectNames(this.properties.isUniqueNames());
		return exporter;
	}

	/**
	 * Creates an instance of {@link ParentAwareNamingStrategy} as the default
	 * {@link ObjectNamingStrategy} bean if no other bean of type
	 * {@link ObjectNamingStrategy} is present.
	 *
	 * The {@link ParentAwareNamingStrategy} is initialized with an
	 * {@link AnnotationJmxAttributeSource} and configured with the default domain and
	 * unique runtime object names based on the properties.
	 * @return the created {@link ParentAwareNamingStrategy} bean
	 */
	@Bean
	@ConditionalOnMissingBean(value = ObjectNamingStrategy.class, search = SearchStrategy.CURRENT)
	public ParentAwareNamingStrategy objectNamingStrategy() {
		ParentAwareNamingStrategy namingStrategy = new ParentAwareNamingStrategy(new AnnotationJmxAttributeSource());
		String defaultDomain = this.properties.getDefaultDomain();
		if (StringUtils.hasLength(defaultDomain)) {
			namingStrategy.setDefaultDomain(defaultDomain);
		}
		namingStrategy.setEnsureUniqueRuntimeObjectNames(this.properties.isUniqueNames());
		return namingStrategy;
	}

	/**
	 * Creates and returns an instance of MBeanServer if it does not already exist. If an
	 * existing MBeanServer is found, it will be located and returned.
	 * @return the MBeanServer instance
	 */
	@Bean
	@ConditionalOnMissingBean
	public MBeanServer mbeanServer() {
		MBeanServerFactoryBean factory = new MBeanServerFactoryBean();
		factory.setLocateExistingServerIfPossible(true);
		factory.afterPropertiesSet();
		return factory.getObject();
	}

}
