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
package org.springframework.bootstrap.actuate.autoconfigure;

import java.util.ArrayList;
import java.util.Arrays;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.bootstrap.actuate.autoconfigure.ManagementAutoConfiguration.RememberManagementConfiguration;
import org.springframework.bootstrap.actuate.properties.ManagementServerProperties;
import org.springframework.bootstrap.autoconfigure.web.EmbeddedContainerCustomizerConfiguration;
import org.springframework.bootstrap.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.bootstrap.properties.ServerProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.ClassUtils;

/**
 * @author Dave Syer
 * 
 */
@Configuration
@Conditional(RememberManagementConfiguration.class)
@Import(ManagementEndpointsRegistration.class)
public class ManagementAutoConfiguration implements ApplicationContextAware {

	public static final String MEMO_BEAN_NAME = ManagementAutoConfiguration.class
			.getName() + ".MEMO";

	private ApplicationContext parent;
	private ConfigurableApplicationContext context;

	@Autowired
	private ServerProperties configuration = new ServerProperties();

	@Autowired
	private ManagementServerProperties management = new ManagementServerProperties();

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.parent = applicationContext;
	}

	@Bean
	public ApplicationListener<ContextClosedEvent> managementContextClosedListener() {
		return new ApplicationListener<ContextClosedEvent>() {
			@Override
			public void onApplicationEvent(ContextClosedEvent event) {
				if (event.getSource() != ManagementAutoConfiguration.this.parent) {
					return;
				}
				if (ManagementAutoConfiguration.this.context != null) {
					ManagementAutoConfiguration.this.context.close();
				}
			}
		};
	}

	@Bean
	public ApplicationListener<ContextRefreshedEvent> managementContextRefeshedListener() {

		return new ApplicationListener<ContextRefreshedEvent>() {

			@Override
			public void onApplicationEvent(ContextRefreshedEvent event) {

				if (event.getSource() != ManagementAutoConfiguration.this.parent) {
					return;
				}

				if (ManagementAutoConfiguration.this.configuration.getPort() != ManagementAutoConfiguration.this.management
						.getPort()) {
					AnnotationConfigEmbeddedWebApplicationContext context = new AnnotationConfigEmbeddedWebApplicationContext();
					context.setParent(ManagementAutoConfiguration.this.parent);
					context.setEnvironment((ConfigurableEnvironment) ManagementAutoConfiguration.this.parent
							.getEnvironment());
					context.register(assembleConfigClasses(ManagementAutoConfiguration.this.parent));
					context.refresh();
					ManagementAutoConfiguration.this.context = context;

				}
			}

		};

	}

	protected static class RememberManagementConfiguration implements Condition {

		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
			Environment environment = context.getEnvironment();
			int serverPort = environment.getProperty("server.port", Integer.class, 8080);
			int managementPort = environment.getProperty("management.port",
					Integer.class, serverPort);
			if (!context.getBeanFactory().containsSingleton(MEMO_BEAN_NAME)) {
				context.getBeanFactory().registerSingleton(MEMO_BEAN_NAME,
						managementPort > 0);
			}
			return managementPort > 0;
		}

	}

	protected Class<?>[] assembleConfigClasses(BeanFactory parent) {

		// Some basic context configuration that all child context need
		ArrayList<Class<?>> configs = new ArrayList<Class<?>>(Arrays.<Class<?>> asList(
				EmbeddedContainerCustomizerConfiguration.class,
				ManagementServerConfiguration.class, ErrorConfiguration.class));

		String managementContextBeanName = OnManagementContextCondition.class.getName();

		// Management context only beans pulled in from the deferred list in the parent
		// context
		if (parent.containsBean(managementContextBeanName)) {
			String[] names = parent.getBean(managementContextBeanName, String[].class);
			for (String name : names) {
				try {
					configs.add(ClassUtils.forName(name,
							ManagementAutoConfiguration.this.parent.getClassLoader()));
				} catch (ClassNotFoundException e) {
					throw new BeanCreationException(managementContextBeanName,
							"Class not found: " + name);
				}
			}
		}

		return configs.toArray(new Class<?>[configs.size()]);

	}

}
