/*
 * Copyright 2012-2019 the original author or authors.
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
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableMBeanExport;
import org.springframework.context.annotation.MBeanExportConfiguration.SpecificPlatform;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.jmx.export.MBeanExporter;
import org.springframework.jmx.export.annotation.AnnotationJmxAttributeSource;
import org.springframework.jmx.export.annotation.AnnotationMBeanExporter;
import org.springframework.jmx.export.naming.ObjectNamingStrategy;
import org.springframework.jmx.support.MBeanServerFactoryBean;
import org.springframework.jmx.support.RegistrationPolicy;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} to enable/disable Spring's
 * {@link EnableMBeanExport} mechanism based on configuration properties.
 * <p>
 * To enable auto export of annotation beans set {@code spring.jmx.enabled: true}.
 *
 * @author Christian Dupuis
 * @author Madhura Bhave
 * @author Artsiom Yudovin
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ MBeanExporter.class })
@ConditionalOnProperty(prefix = "spring.jmx", name = "enabled", havingValue = "true")
public class JmxAutoConfiguration {

	private final Environment environment;

	public JmxAutoConfiguration(Environment environment) {
		this.environment = environment;
	}

	@Bean
	@Primary
	@ConditionalOnMissingBean(value = MBeanExporter.class, search = SearchStrategy.CURRENT)
	public AnnotationMBeanExporter mbeanExporter(ObjectNamingStrategy namingStrategy,
			BeanFactory beanFactory) {
		AnnotationMBeanExporter exporter = new AnnotationMBeanExporter();
		exporter.setRegistrationPolicy(RegistrationPolicy.FAIL_ON_EXISTING);
		exporter.setNamingStrategy(namingStrategy);
		String serverBean = this.environment.getProperty("spring.jmx.server",
				"mbeanServer");
		if (StringUtils.hasLength(serverBean)) {
			exporter.setServer(beanFactory.getBean(serverBean, MBeanServer.class));
		}
		return exporter;
	}

	@Bean
	@ConditionalOnMissingBean(value = ObjectNamingStrategy.class, search = SearchStrategy.CURRENT)
	public ParentAwareNamingStrategy objectNamingStrategy() {
		ParentAwareNamingStrategy namingStrategy = new ParentAwareNamingStrategy(
				new AnnotationJmxAttributeSource());
		String defaultDomain = this.environment.getProperty("spring.jmx.default-domain");
		if (StringUtils.hasLength(defaultDomain)) {
			namingStrategy.setDefaultDomain(defaultDomain);
		}
		boolean uniqueNames = this.environment.getProperty("spring.jmx.unique-names",
				Boolean.class, false);
		namingStrategy.setEnsureUniqueRuntimeObjectNames(uniqueNames);
		return namingStrategy;
	}

	@Bean
	@ConditionalOnMissingBean
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

}
