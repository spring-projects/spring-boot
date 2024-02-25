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

package org.springframework.boot.test.autoconfigure.web.servlet;

import java.util.HashMap;
import java.util.Map;

import org.openqa.selenium.WebDriver;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.Scope;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * A special scope used for {@link WebDriver} beans. Usually registered by a
 * {@link WebDriverContextCustomizerFactory} and reset by a
 * {@link WebDriverTestExecutionListener}.
 *
 * @author Phillip Webb
 * @since 3.0.0
 * @see WebDriverContextCustomizerFactory
 * @see WebDriverTestExecutionListener
 */
public class WebDriverScope implements Scope {

	/**
	 * WebDriver bean scope name.
	 */
	public static final String NAME = "webDriver";

	private static final String WEB_DRIVER_CLASS = "org.openqa.selenium.WebDriver";

	private static final String[] BEAN_CLASSES = { WEB_DRIVER_CLASS,
			"org.springframework.test.web.servlet.htmlunit.webdriver.MockMvcHtmlUnitDriverBuilder" };

	private final Map<String, Object> instances = new HashMap<>();

	/**
     * Retrieves an object from the scope with the given name.
     * If the object does not exist in the scope, it creates a new instance using the provided object factory.
     * The created instance is then stored in the scope for future retrieval.
     *
     * @param name          the name of the object to retrieve
     * @param objectFactory the factory used to create a new instance if the object does not exist in the scope
     * @return the retrieved or newly created instance
     */
    @Override
	public Object get(String name, ObjectFactory<?> objectFactory) {
		synchronized (this.instances) {
			Object instance = this.instances.get(name);
			if (instance == null) {
				instance = objectFactory.getObject();
				this.instances.put(name, instance);
			}
			return instance;
		}
	}

	/**
     * Removes an instance from the WebDriverScope using the specified name.
     * 
     * @param name the name of the instance to be removed
     * @return the removed instance, or null if no instance with the specified name exists
     */
    @Override
	public Object remove(String name) {
		synchronized (this.instances) {
			return this.instances.remove(name);
		}
	}

	/**
     * Registers a destruction callback for the given name.
     * 
     * @param name     the name of the object to register the destruction callback for
     * @param callback the callback to be executed when the object is destroyed
     */
    @Override
	public void registerDestructionCallback(String name, Runnable callback) {
	}

	/**
     * Resolves the contextual object for the given key.
     * 
     * @param key the key for the contextual object
     * @return the resolved contextual object, or null if not found
     */
    @Override
	public Object resolveContextualObject(String key) {
		return null;
	}

	/**
     * Returns the conversation ID associated with this WebDriverScope.
     * 
     * @return the conversation ID, or null if not available
     */
    @Override
	public String getConversationId() {
		return null;
	}

	/**
	 * Reset all instances in the scope.
	 * @return {@code true} if items were reset
	 */
	boolean reset() {
		boolean reset = false;
		synchronized (this.instances) {
			for (Object instance : this.instances.values()) {
				reset = true;
				if (instance instanceof WebDriver webDriver) {
					webDriver.quit();
				}
			}
			this.instances.clear();
		}
		return reset;
	}

	/**
	 * Register this scope with the specified context and reassign appropriate bean
	 * definitions to use it.
	 * @param context the application context
	 */
	static void registerWith(ConfigurableApplicationContext context) {
		if (!ClassUtils.isPresent(WEB_DRIVER_CLASS, null)) {
			return;
		}
		ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
		if (beanFactory.getRegisteredScope(NAME) == null) {
			beanFactory.registerScope(NAME, new WebDriverScope());
		}
		context.addBeanFactoryPostProcessor(WebDriverScope::postProcessBeanFactory);
	}

	/**
     * This method is used to post-process the bean factory by setting the scope of certain bean definitions.
     * It iterates over a list of bean classes and for each bean class, it retrieves the bean names from the bean factory
     * that match the resolved class name. For each matching bean definition, if the scope is not already set, it sets the
     * scope to "NAME".
     *
     * @param beanFactory The configurable listable bean factory to be post-processed.
     */
    private static void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		for (String beanClass : BEAN_CLASSES) {
			for (String beanName : beanFactory.getBeanNamesForType(ClassUtils.resolveClassName(beanClass, null))) {
				BeanDefinition definition = beanFactory.getBeanDefinition(beanName);
				if (!StringUtils.hasLength(definition.getScope())) {
					definition.setScope(NAME);
				}
			}
		}
	}

	/**
	 * Return the {@link WebDriverScope} being used by the specified context (if any).
	 * @param context the application context
	 * @return the web driver scope or {@code null}
	 */
	static WebDriverScope getFrom(ApplicationContext context) {
		if (context instanceof ConfigurableApplicationContext configurableContext) {
			Scope scope = configurableContext.getBeanFactory().getRegisteredScope(NAME);
			return (scope instanceof WebDriverScope webDriverScope) ? webDriverScope : null;
		}
		return null;
	}

}
