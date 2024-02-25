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

package org.springframework.boot.actuate.autoconfigure.web;

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

/**
 * Factory for creating a separate management context when the management web server is
 * running on a different port to the main application.
 * <p>
 * <strong>For internal use only.</strong>
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 3.0.0
 */
public final class ManagementContextFactory {

	private final WebApplicationType webApplicationType;

	private final Class<? extends WebServerFactory> webServerFactoryClass;

	private final Class<?>[] autoConfigurationClasses;

	/**
     * Constructs a new ManagementContextFactory with the specified parameters.
     *
     * @param webApplicationType the type of web application
     * @param webServerFactoryClass the class of the web server factory
     * @param autoConfigurationClasses the auto-configuration classes
     */
    public ManagementContextFactory(WebApplicationType webApplicationType,
			Class<? extends WebServerFactory> webServerFactoryClass, Class<?>... autoConfigurationClasses) {
		this.webApplicationType = webApplicationType;
		this.webServerFactoryClass = webServerFactoryClass;
		this.autoConfigurationClasses = autoConfigurationClasses;
	}

	/**
     * Creates a management context with the given parent context.
     * 
     * @param parentContext the parent application context
     * @return the created management context
     */
    public ConfigurableApplicationContext createManagementContext(ApplicationContext parentContext) {
		Environment parentEnvironment = parentContext.getEnvironment();
		ConfigurableEnvironment childEnvironment = ApplicationContextFactory.DEFAULT
			.createEnvironment(this.webApplicationType);
		if (parentEnvironment instanceof ConfigurableEnvironment configurableEnvironment) {
			childEnvironment.setConversionService((configurableEnvironment).getConversionService());
		}
		ConfigurableApplicationContext managementContext = ApplicationContextFactory.DEFAULT
			.create(this.webApplicationType);
		managementContext.setEnvironment(childEnvironment);
		managementContext.setParent(parentContext);
		return managementContext;
	}

	/**
     * Registers the web server factory beans in the given management context.
     * 
     * @param parentContext the parent application context
     * @param managementContext the management application context
     * @param registry the annotation config registry
     */
    public void registerWebServerFactoryBeans(ApplicationContext parentContext,
			ConfigurableApplicationContext managementContext, AnnotationConfigRegistry registry) {
		registry.register(this.autoConfigurationClasses);
		registerWebServerFactoryFromParent(parentContext, managementContext);
	}

	/**
     * Registers the web server factory from the parent application context into the management context.
     * 
     * @param parentContext The parent application context.
     * @param managementContext The management context.
     */
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

	/**
     * Determines the web server factory class to be used for the management context.
     * 
     * @param parent the parent application context
     * @return the web server factory class
     * @throws NoSuchBeanDefinitionException if the web server factory class is not found in the parent application context
     * @throws FatalBeanException if the web server factory class cannot be instantiated
     */
    private Class<?> determineWebServerFactoryClass(ApplicationContext parent) throws NoSuchBeanDefinitionException {
		Class<?> factoryClass = parent.getBean(this.webServerFactoryClass).getClass();
		if (cannotBeInstantiated(factoryClass)) {
			throw new FatalBeanException("ManagementContextWebServerFactory implementation " + factoryClass.getName()
					+ " cannot be instantiated. To allow a separate management port to be used, a top-level class "
					+ "or static inner class should be used instead");
		}
		return factoryClass;
	}

	/**
     * Determines if the given factory class cannot be instantiated.
     * 
     * @param factoryClass the factory class to check
     * @return true if the factory class cannot be instantiated, false otherwise
     */
    private boolean cannotBeInstantiated(Class<?> factoryClass) {
		return factoryClass.isLocalClass()
				|| (factoryClass.isMemberClass() && !Modifier.isStatic(factoryClass.getModifiers()))
				|| factoryClass.isAnonymousClass();
	}

}
