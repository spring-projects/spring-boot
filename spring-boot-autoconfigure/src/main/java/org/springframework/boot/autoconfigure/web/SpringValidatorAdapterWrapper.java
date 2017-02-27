/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure.web;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;

/**
 * Wraps a {@link SpringValidatorAdapter} so that only the Spring's {@link Validator} type
 * is exposed. This prevents such a bean to expose both the Spring and JSR-303 validator
 * contract at the same time.
 *
 * @author Stephane Nicoll
 */
class SpringValidatorAdapterWrapper
		implements Validator, ApplicationContextAware, InitializingBean, DisposableBean {

	private final SpringValidatorAdapter target;

	private final boolean managed;

	SpringValidatorAdapterWrapper(SpringValidatorAdapter target, boolean managed) {
		this.target = target;
		this.managed = managed;
	}

	public SpringValidatorAdapter getTarget() {
		return this.target;
	}

	@Override
	public boolean supports(Class<?> clazz) {
		return this.target.supports(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		this.target.validate(target, errors);
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		if (!this.managed && this.target instanceof ApplicationContextAware) {
			((ApplicationContextAware) this.target)
					.setApplicationContext(applicationContext);
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (!this.managed && this.target instanceof InitializingBean) {
			((InitializingBean) this.target).afterPropertiesSet();
		}
	}

	@Override
	public void destroy() throws Exception {
		if (!this.managed && this.target instanceof DisposableBean) {
			((DisposableBean) this.target).destroy();
		}
	}

}
