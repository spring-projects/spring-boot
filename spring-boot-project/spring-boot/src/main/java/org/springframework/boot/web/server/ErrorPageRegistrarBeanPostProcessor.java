/*
 * Copyright 2012-2022 the original author or authors.
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
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.util.Assert;

/**
 * {@link BeanPostProcessor} that applies all {@link ErrorPageRegistrar}s from the bean
 * factory to {@link ErrorPageRegistry} beans.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public class ErrorPageRegistrarBeanPostProcessor implements BeanPostProcessor, BeanFactoryAware {

	private ListableBeanFactory beanFactory;

	private List<ErrorPageRegistrar> registrars;

	/**
	 * Set the BeanFactory that this bean runs in.
	 * <p>
	 * Invoked after population of normal bean properties but before an init callback such
	 * as InitializingBean's afterPropertiesSet or a custom init-method. Invoked after
	 * ResourceLoaderAware's setResourceLoader.
	 * <p>
	 * This method allows the bean instance to perform initialization based on its bean
	 * factory context, such as setting up bean references or preparing the bean for use.
	 * <p>
	 * This implementation checks if the provided beanFactory is an instance of
	 * ListableBeanFactory and throws an IllegalArgumentException if it is not. It then
	 * assigns the provided beanFactory to the beanFactory instance variable.
	 * @param beanFactory the BeanFactory that this bean runs in
	 * @throws IllegalArgumentException if the provided beanFactory is not an instance of
	 * ListableBeanFactory
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		Assert.isInstanceOf(ListableBeanFactory.class, beanFactory,
				"ErrorPageRegistrarBeanPostProcessor can only be used with a ListableBeanFactory");
		this.beanFactory = (ListableBeanFactory) beanFactory;
	}

	/**
	 * This method is called before the initialization of a bean. It checks if the bean is
	 * an instance of ErrorPageRegistry and if so, it calls the
	 * postProcessBeforeInitialization method with the errorPageRegistry as a parameter.
	 * @param bean the bean object being initialized
	 * @param beanName the name of the bean
	 * @return the initialized bean object
	 * @throws BeansException if an error occurs during the initialization process
	 */
	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof ErrorPageRegistry errorPageRegistry) {
			postProcessBeforeInitialization(errorPageRegistry);
		}
		return bean;
	}

	/**
	 * This method is called after the initialization of a bean.
	 * @param bean The initialized bean.
	 * @param beanName The name of the bean.
	 * @return The initialized bean.
	 * @throws BeansException If an error occurs during the post-processing of the bean.
	 */
	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	/**
	 * Post-processes the ErrorPageRegistry before initialization.
	 * @param registry the ErrorPageRegistry to be processed
	 */
	private void postProcessBeforeInitialization(ErrorPageRegistry registry) {
		for (ErrorPageRegistrar registrar : getRegistrars()) {
			registrar.registerErrorPages(registry);
		}
	}

	/**
	 * Retrieves the collection of ErrorPageRegistrar instances. If the collection is not
	 * already initialized, it initializes it by looking up the ErrorPageRegistrar beans
	 * from the bean factory and sorting them based on their order. The resulting
	 * collection is then made unmodifiable.
	 * @return the collection of ErrorPageRegistrar instances
	 */
	private Collection<ErrorPageRegistrar> getRegistrars() {
		if (this.registrars == null) {
			// Look up does not include the parent context
			this.registrars = new ArrayList<>(
					this.beanFactory.getBeansOfType(ErrorPageRegistrar.class, false, false).values());
			this.registrars.sort(AnnotationAwareOrderComparator.INSTANCE);
			this.registrars = Collections.unmodifiableList(this.registrars);
		}
		return this.registrars;
	}

}
