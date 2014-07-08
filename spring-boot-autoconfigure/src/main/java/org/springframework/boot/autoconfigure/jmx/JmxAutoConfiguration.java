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

package org.springframework.boot.autoconfigure.jmx;

import javax.management.MBeanServer;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableMBeanExport;
import org.springframework.context.annotation.MBeanExportConfiguration;
import org.springframework.core.env.Environment;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.jmx.export.MBeanExporter;
import org.springframework.jmx.export.annotation.AnnotationJmxAttributeSource;
import org.springframework.jmx.export.annotation.AnnotationMBeanExporter;
import org.springframework.jmx.export.naming.ObjectNamingStrategy;
import org.springframework.jmx.support.MBeanServerFactoryBean;
import org.springframework.jmx.support.WebSphereMBeanServerFactoryBean;
import org.springframework.jndi.JndiObjectFactoryBean;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} to enable/disable Spring's
 * {@link EnableMBeanExport} mechanism based on configuration properties.
 * <p>
 * To disable auto export of annotation beans set <code>spring.jmx.enabled: false</code>.
 *
 * @author Christian Dupuis
 */
@Configuration
@ConditionalOnClass({ MBeanExporter.class })
@ConditionalOnExpression("${spring.jmx.enabled:true}")
public class JmxAutoConfiguration {

	@Autowired
	private Environment environment;

	@Autowired
	private BeanFactory beanFactory;

	@Autowired
	private ObjectNamingStrategy namingStrategy;

	@Bean
	@ConditionalOnMissingBean(value = MBeanExporter.class, search = SearchStrategy.CURRENT)
	public AnnotationMBeanExporter mbeanExporter() {
		// Re-use the @EnableMBeanExport configuration
		MBeanExportConfiguration config = new MBeanExportConfiguration();
		config.setEnvironment(this.environment);
		config.setBeanFactory(this.beanFactory);
		config.setImportMetadata(new StandardAnnotationMetadata(Empty.class));
		// But add a custom naming strategy
		AnnotationMBeanExporter exporter = config.mbeanExporter();
		exporter.setNamingStrategy(this.namingStrategy);
		return exporter;
	}

	@Bean
	@ConditionalOnMissingBean(ObjectNamingStrategy.class)
	public ParentAwareNamingStrategy objectNamingStrategy() {
		return new ParentAwareNamingStrategy(new AnnotationJmxAttributeSource());
	}

	@Bean
	@ConditionalOnMissingBean(MBeanServer.class)
	public MBeanServer mbeanServer() {
		SpecificPlatform platform = SpecificPlatform.get();
		if (platform != null) {
			return platform.getMBeanServer();
		}
		MBeanServerFactoryBean factory = new MBeanServerFactoryBean();
		factory.setLocateExistingServerIfPossible(true);
		factory.afterPropertiesSet();
		return factory.getObject();

	}

	@EnableMBeanExport(defaultDomain = "${spring.jmx.default_domain:}", server = "${spring.jmx.server:mbeanServer}")
	private static class Empty {

	}

	// Copied and adapted from MBeanExportConfiguration
	private static enum SpecificPlatform {

		WEBLOGIC("weblogic.management.Helper") {
			@Override
			public FactoryBean<?> getMBeanServerFactory() {
				JndiObjectFactoryBean factory = new JndiObjectFactoryBean();
				factory.setJndiName("java:comp/env/jmx/runtime");
				return factory;
			}
		},

		WEBSPHERE("com.ibm.websphere.management.AdminServiceFactory") {
			@Override
			public FactoryBean<MBeanServer> getMBeanServerFactory() {
				return new WebSphereMBeanServerFactoryBean();
			}
		};

		private final String identifyingClass;

		private SpecificPlatform(String identifyingClass) {
			this.identifyingClass = identifyingClass;
		}

		public MBeanServer getMBeanServer() {
			try {
				FactoryBean<?> factory = getMBeanServerFactory();
				if (factory instanceof InitializingBean) {
					((InitializingBean) factory).afterPropertiesSet();
				}
				Object server = factory.getObject();
				Assert.isInstanceOf(MBeanServer.class, server);
				return (MBeanServer) server;
			}
			catch (Exception ex) {
				throw new IllegalStateException(ex);
			}
		}

		protected abstract FactoryBean<?> getMBeanServerFactory();

		public static SpecificPlatform get() {
			ClassLoader classLoader = MBeanExportConfiguration.class.getClassLoader();
			for (SpecificPlatform environment : values()) {
				if (ClassUtils.isPresent(environment.identifyingClass, classLoader)) {
					return environment;
				}
			}
			return null;
		}

	}

}
