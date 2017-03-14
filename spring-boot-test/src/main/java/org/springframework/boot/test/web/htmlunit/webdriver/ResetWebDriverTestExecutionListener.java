/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.test.web.htmlunit.webdriver;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.openqa.selenium.WebDriver;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/**
 * {@link TestExecutionListener} to quit after test method and instantiate before test
 * method.
 *
 * @author Alex Fainshtein
 * @since 1.4.0
 */
public class ResetWebDriverTestExecutionListener extends AbstractTestExecutionListener {

	@Override
	public void afterTestMethod(TestContext testContext) throws Exception {
		ApplicationContext applicationContext = testContext.getApplicationContext();
		if (applicationContext instanceof ConfigurableApplicationContext) {
			ConfigurableApplicationContext configurableApplicationContext = (ConfigurableApplicationContext) applicationContext;
			ConfigurableListableBeanFactory beanFactory = configurableApplicationContext
					.getBeanFactory();
			String[] names = beanFactory.getBeanDefinitionNames();
			Set<String> instantiatedSingletons = new HashSet<String>(
					Arrays.asList(beanFactory.getSingletonNames()));
			for (String name : names) {
				BeanDefinition definition = beanFactory.getBeanDefinition(name);
				if (definition.isSingleton() && instantiatedSingletons.contains(name)) {
					Object bean = beanFactory.getBean(name);
					if (bean instanceof WebDriver) {
						WebDriver webdriver = (WebDriver) bean;
						webdriver.quit();
						testContext
								.markApplicationContextDirty(HierarchyMode.CURRENT_LEVEL);
					}
				}
			}
		}
	}

}
