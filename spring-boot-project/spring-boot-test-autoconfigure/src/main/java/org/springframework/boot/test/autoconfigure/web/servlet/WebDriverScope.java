/*
 * Copyright 2012-2018 the original author or authors.
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
 * @see WebDriverContextCustomizerFactory
 * @see WebDriverTestExecutionListener
 */
class WebDriverScope implements Scope {

	public static final String NAME = "webDriver";

	private static final String WEB_DRIVER_CLASS = "org.openqa.selenium.WebDriver";

	private static final String[] BEAN_CLASSES = { WEB_DRIVER_CLASS,
			"org.springframework.test.web.servlet.htmlunit.webdriver.MockMvcHtmlUnitDriverBuilder" };

	private final Map<String, Object> instances = new HashMap<>();

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

	@Override
	public Object remove(String name) {
		synchronized (this.instances) {
			return this.instances.remove(name);
		}
	}

	@Override
	public void registerDestructionCallback(String name, Runnable callback) {
	}

	@Override
	public Object resolveContextualObject(String key) {
		return null;
	}

	@Override
	public String getConversationId() {
		return null;
	}

	/**
	 * Reset all instances in the scope.
	 * @return {@code true} if items were reset
	 */
	public boolean reset() {
		boolean reset = false;
		synchronized (this.instances) {
			for (Object instance : this.instances.values()) {
				reset = true;
				if (instance instanceof WebDriver) {
					((WebDriver) instance).quit();
				}
			}
			this.instances.clear();
		}
		return reset;
	}

	/**
	 * Register this scope with the specified context and reassign appropriate bean
	 * definitions to used it.
	 * @param context the application context
	 */
	public static void registerWith(ConfigurableApplicationContext context) {
		if (!ClassUtils.isPresent(WEB_DRIVER_CLASS, null)) {
			return;
		}
		ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
		if (beanFactory.getRegisteredScope(NAME) == null) {
			beanFactory.registerScope(NAME, new WebDriverScope());
		}
		context.addBeanFactoryPostProcessor(WebDriverScope::postProcessBeanFactory);
	}

	private static void postProcessBeanFactory(
			ConfigurableListableBeanFactory beanFactory) {
		for (String beanClass : BEAN_CLASSES) {
			for (String beanName : beanFactory
					.getBeanNamesForType(ClassUtils.resolveClassName(beanClass, null))) {
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
	public static WebDriverScope getFrom(ApplicationContext context) {
		if (context instanceof ConfigurableApplicationContext) {
			Scope scope = ((ConfigurableApplicationContext) context).getBeanFactory()
					.getRegisteredScope(NAME);
			return (scope instanceof WebDriverScope) ? (WebDriverScope) scope : null;
		}
		return null;
	}

}
