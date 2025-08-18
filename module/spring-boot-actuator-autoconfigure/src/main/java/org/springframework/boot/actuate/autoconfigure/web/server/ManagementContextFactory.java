/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.web.server;

import java.lang.reflect.Modifier;

import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.ApplicationContextFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.web.server.WebServerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigRegistry;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.util.Assert;

/**
 * Factory for creating a separate management context when the management web server is
 * running on a different port to the main application.
 * <p>
 * <strong>For internal use only.</strong>
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Yongjun Hong
 * @since 4.0.0
 */
public final class ManagementContextFactory {

	private final WebApplicationType webApplicationType;

	private final Class<? extends WebServerFactory> webServerFactoryClass;

	private final Class<?>[] autoConfigurationClasses;

	public ManagementContextFactory(WebApplicationType webApplicationType,
			Class<? extends WebServerFactory> webServerFactoryClass, Class<?>... autoConfigurationClasses) {
		this.webApplicationType = webApplicationType;
		this.webServerFactoryClass = webServerFactoryClass;
		this.autoConfigurationClasses = autoConfigurationClasses;
	}

	public ConfigurableApplicationContext createManagementContext(ApplicationContext parentContext) {
		Environment parentEnvironment = parentContext.getEnvironment();
		ConfigurableEnvironment childEnvironment = ApplicationContextFactory.DEFAULT
			.createEnvironment(this.webApplicationType);
		Assert.state(childEnvironment != null, "'childEnvironment' must not be null");
		if (parentEnvironment instanceof ConfigurableEnvironment configurableEnvironment) {
			configurableEnvironment.getPropertySources().forEach(propertySource -> {
				if (isManagementPropertySource(propertySource, childEnvironment)) {
					childEnvironment.getPropertySources().addLast(propertySource);
				}
			});
		}
		ConfigurableApplicationContext managementContext = ApplicationContextFactory.DEFAULT
			.create(this.webApplicationType);
		Assert.state(managementContext != null, "'managementContext' must not be null");
		managementContext.setEnvironment(childEnvironment);
		managementContext.setParent(parentContext);
		return managementContext;
	}

	private boolean isManagementPropertySource(PropertySource<?> propertySource, ConfigurableEnvironment childEnvironment) {
		return propertySource.getName().contains("management")
				&& !childEnvironment.getPropertySources().contains(propertySource.getName());
	}

	public void registerWebServerFactoryBeans(ApplicationContext parentContext,
			ConfigurableApplicationContext managementContext, AnnotationConfigRegistry registry) {
		if (this.autoConfigurationClasses != null && this.autoConfigurationClasses.length > 0) {
			registry.register(this.autoConfigurationClasses);
		}
		registerWebServerFactoryFromParent(parentContext, managementContext);
	}

	private void registerWebServerFactoryFromParent(ApplicationContext parentContext,
			ConfigurableApplicationContext managementContext) {
		try {
			if (managementContext.getBeanFactory() instanceof BeanDefinitionRegistry registry) {
				registry.registerBeanDefinition("ManagementContextWebServerFactory",
						new RootBeanDefinition(determineWebServerFactoryClass(parentContext)));
			}
		}
		catch (NoSuchBeanDefinitionException ex) {
			// Ignore and assume auto-configuration
		}
	}

	private Class<?> determineWebServerFactoryClass(ApplicationContext parent) throws NoSuchBeanDefinitionException {
		Class<?> factoryClass = parent.getBean(this.webServerFactoryClass).getClass();
		if (cannotBeInstantiated(factoryClass)) {
			throw new FatalBeanException("ManagementContextWebServerFactory implementation " + factoryClass.getName()
					+ " cannot be instantiated. To allow a separate management port to be used, a top-level class "
					+ "or static inner class should be used instead");
		}
		return factoryClass;
	}

	private boolean cannotBeInstantiated(Class<?> factoryClass) {
		return factoryClass.isLocalClass()
				|| (factoryClass.isMemberClass() && !Modifier.isStatic(factoryClass.getModifiers()))
				|| factoryClass.isAnonymousClass();
	}

}
