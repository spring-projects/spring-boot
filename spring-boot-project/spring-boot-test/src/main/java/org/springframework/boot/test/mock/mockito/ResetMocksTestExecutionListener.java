/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.test.mock.mockito;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.mockito.Mockito;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.NativeDetector;
import org.springframework.core.Ordered;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.util.ClassUtils;

/**
 * {@link TestExecutionListener} to reset any mock beans that have been marked with a
 * {@link MockReset}. Typically used alongside {@link MockitoTestExecutionListener}.
 *
 * @author Phillip Webb
 * @since 1.4.0
 * @see MockitoTestExecutionListener
 */
public class ResetMocksTestExecutionListener extends AbstractTestExecutionListener {

	private static final boolean MOCKITO_IS_PRESENT = ClassUtils.isPresent("org.mockito.MockSettings",
			ResetMocksTestExecutionListener.class.getClassLoader());

	/**
     * Returns the order of this ResetMocksTestExecutionListener.
     * The order is determined by subtracting 100 from the lowest precedence.
     *
     * @return the order of this ResetMocksTestExecutionListener
     */
    @Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE - 100;
	}

	/**
     * This method is called before each test method is executed.
     * It checks if Mockito is present and if the application is not running in a native image.
     * If both conditions are met, it resets the mocks in the application context using the MockReset.BEFORE strategy.
     *
     * @param testContext the TestContext object representing the current test context
     * @throws Exception if an error occurs during the execution of the method
     */
    @Override
	public void beforeTestMethod(TestContext testContext) throws Exception {
		if (MOCKITO_IS_PRESENT && !NativeDetector.inNativeImage()) {
			resetMocks(testContext.getApplicationContext(), MockReset.BEFORE);
		}
	}

	/**
     * This method is called after each test method is executed.
     * It checks if Mockito is present and if the application is not running in a native image.
     * If both conditions are met, it resets the mocks in the application context using the MockReset.AFTER strategy.
     *
     * @param testContext the test context containing information about the test being executed
     * @throws Exception if an error occurs during the reset of mocks
     */
    @Override
	public void afterTestMethod(TestContext testContext) throws Exception {
		if (MOCKITO_IS_PRESENT && !NativeDetector.inNativeImage()) {
			resetMocks(testContext.getApplicationContext(), MockReset.AFTER);
		}
	}

	/**
     * Resets the mocks in the given ApplicationContext.
     * 
     * @param applicationContext the ApplicationContext to reset the mocks in
     * @param reset the MockReset strategy to use for resetting the mocks
     */
    private void resetMocks(ApplicationContext applicationContext, MockReset reset) {
		if (applicationContext instanceof ConfigurableApplicationContext configurableContext) {
			resetMocks(configurableContext, reset);
		}
	}

	/**
     * Resets all the mocked beans in the application context based on the given reset type.
     * 
     * @param applicationContext the configurable application context
     * @param reset the reset type for the mocked beans
     */
    private void resetMocks(ConfigurableApplicationContext applicationContext, MockReset reset) {
		ConfigurableListableBeanFactory beanFactory = applicationContext.getBeanFactory();
		String[] names = beanFactory.getBeanDefinitionNames();
		Set<String> instantiatedSingletons = new HashSet<>(Arrays.asList(beanFactory.getSingletonNames()));
		for (String name : names) {
			BeanDefinition definition = beanFactory.getBeanDefinition(name);
			if (definition.isSingleton() && instantiatedSingletons.contains(name)) {
				Object bean = getBean(beanFactory, name);
				if (bean != null && reset.equals(MockReset.get(bean))) {
					Mockito.reset(bean);
				}
			}
		}
		try {
			MockitoBeans mockedBeans = beanFactory.getBean(MockitoBeans.class);
			for (Object mockedBean : mockedBeans) {
				if (reset.equals(MockReset.get(mockedBean))) {
					Mockito.reset(mockedBean);
				}
			}
		}
		catch (NoSuchBeanDefinitionException ex) {
			// Continue
		}
		if (applicationContext.getParent() != null) {
			resetMocks(applicationContext.getParent(), reset);
		}
	}

	/**
     * Retrieves a bean from the given bean factory by name.
     * 
     * @param beanFactory the configurable listable bean factory
     * @param name the name of the bean to retrieve
     * @return the retrieved bean
     */
    private Object getBean(ConfigurableListableBeanFactory beanFactory, String name) {
		try {
			if (isStandardBeanOrSingletonFactoryBean(beanFactory, name)) {
				return beanFactory.getBean(name);
			}
		}
		catch (Exception ex) {
			// Continue
		}
		return beanFactory.getSingleton(name);
	}

	/**
     * Checks if the bean with the given name is a standard bean or a singleton factory bean.
     * 
     * @param beanFactory the bean factory to check
     * @param name the name of the bean to check
     * @return true if the bean is a standard bean or a singleton factory bean, false otherwise
     */
    private boolean isStandardBeanOrSingletonFactoryBean(ConfigurableListableBeanFactory beanFactory, String name) {
		String factoryBeanName = BeanFactory.FACTORY_BEAN_PREFIX + name;
		if (beanFactory.containsBean(factoryBeanName)) {
			FactoryBean<?> factoryBean = (FactoryBean<?>) beanFactory.getBean(factoryBeanName);
			return factoryBean.isSingleton();
		}
		return true;
	}

}
