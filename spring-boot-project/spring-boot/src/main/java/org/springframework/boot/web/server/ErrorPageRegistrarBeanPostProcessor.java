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

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		Assert.isInstanceOf(ListableBeanFactory.class, beanFactory,
				"ErrorPageRegistrarBeanPostProcessor can only be used with a ListableBeanFactory");
		this.beanFactory = (ListableBeanFactory) beanFactory;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof ErrorPageRegistry) {
			postProcessBeforeInitialization((ErrorPageRegistry) bean);
		}
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	private void postProcessBeforeInitialization(ErrorPageRegistry registry) {
		for (ErrorPageRegistrar registrar : getRegistrars()) {
			registrar.registerErrorPages(registry);
		}
	}

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
