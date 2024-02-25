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

package org.springframework.boot.web.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.util.LambdaSafe;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.util.Assert;

/**
 * {@link BeanPostProcessor} that applies all {@link WebServerFactoryCustomizer} beans
 * from the bean factory to {@link WebServerFactory} beans.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public class WebServerFactoryCustomizerBeanPostProcessor implements BeanPostProcessor, BeanFactoryAware {

	private ListableBeanFactory beanFactory;

	private List<WebServerFactoryCustomizer<?>> customizers;

	/**
	 * Set the BeanFactory that this object runs in.
	 * <p>
	 * Invoked after population of normal bean properties but before an init callback such
	 * as InitializingBean's {@code afterPropertiesSet} or a custom init-method. Invoked
	 * after ApplicationContextAware's {@code setApplicationContext}.
	 * <p>
	 * This method allows the bean instance to perform initialization based on its bean
	 * factory context, such as setting up bean references or preparing the bean for use.
	 * <p>
	 * Note that this method will be called on any bean, regardless of whether it also
	 * implements InitializingBean and/or defines a custom init-method.
	 * <p>
	 * This implementation checks if the provided bean factory is an instance of
	 * ListableBeanFactory, and if not, throws an IllegalArgumentException. It then
	 * assigns the provided bean factory to the beanFactory field.
	 * @param beanFactory the BeanFactory that this object runs in
	 * @throws IllegalArgumentException if the provided bean factory is not an instance of
	 * ListableBeanFactory
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		Assert.isInstanceOf(ListableBeanFactory.class, beanFactory,
				"WebServerCustomizerBeanPostProcessor can only be used with a ListableBeanFactory");
		this.beanFactory = (ListableBeanFactory) beanFactory;
	}

	/**
	 * This method is called before the initialization of a bean. It checks if the bean is
	 * an instance of WebServerFactory and if so, it calls the
	 * postProcessBeforeInitialization method with the webServerFactory as a parameter.
	 * @param bean The bean object being initialized.
	 * @param beanName The name of the bean being initialized.
	 * @return The initialized bean object.
	 * @throws BeansException If an error occurs during the initialization process.
	 */
	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof WebServerFactory webServerFactory) {
			postProcessBeforeInitialization(webServerFactory);
		}
		return bean;
	}

	/**
	 * This method is called after the initialization of a bean in the
	 * WebServerFactoryCustomizerBeanPostProcessor class. It allows for any necessary
	 * post-processing of the bean.
	 * @param bean The initialized bean object.
	 * @param beanName The name of the bean.
	 * @return The processed bean object.
	 * @throws BeansException If any exception occurs during the post-processing of the
	 * bean.
	 */
	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	/**
	 * This method is called before the initialization of the web server factory. It
	 * invokes the customize method of each customizer in the list of customizers, passing
	 * the web server factory as a parameter.
	 * @param webServerFactory the web server factory to be customized
	 */
	@SuppressWarnings("unchecked")
	private void postProcessBeforeInitialization(WebServerFactory webServerFactory) {
		LambdaSafe.callbacks(WebServerFactoryCustomizer.class, getCustomizers(), webServerFactory)
			.withLogger(WebServerFactoryCustomizerBeanPostProcessor.class)
			.invoke((customizer) -> customizer.customize(webServerFactory));
	}

	/**
	 * Retrieves the collection of customizers for the web server factory. If the
	 * customizers have not been initialized yet, it initializes them by looking up the
	 * web server factory customizer beans, sorting them based on their order, and making
	 * the collection unmodifiable.
	 * @return the collection of web server factory customizers
	 */
	private Collection<WebServerFactoryCustomizer<?>> getCustomizers() {
		if (this.customizers == null) {
			// Look up does not include the parent context
			this.customizers = new ArrayList<>(getWebServerFactoryCustomizerBeans());
			this.customizers.sort(AnnotationAwareOrderComparator.INSTANCE);
			this.customizers = Collections.unmodifiableList(this.customizers);
		}
		return this.customizers;
	}

	/**
	 * Retrieves all the beans of type WebServerFactoryCustomizer and returns them as a
	 * collection.
	 * @return A collection of WebServerFactoryCustomizer beans.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Collection<WebServerFactoryCustomizer<?>> getWebServerFactoryCustomizerBeans() {
		return (Collection) this.beanFactory.getBeansOfType(WebServerFactoryCustomizer.class, false, false).values();
	}

}
