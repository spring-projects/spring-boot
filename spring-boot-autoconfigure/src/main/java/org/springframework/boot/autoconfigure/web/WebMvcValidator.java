/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure.web;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.validation.MessageInterpolatorFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.validation.Errors;
import org.springframework.validation.SmartValidator;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.OptionalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;

/**
 * A {@link SmartValidator} exposed as a bean for WebMvc use. Wraps existing
 * {@link SpringValidatorAdapter} instances so that only the Spring's {@link Validator}
 * type is exposed. This prevents such a bean to expose both the Spring and JSR-303
 * validator contract at the same time.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
class WebMvcValidator implements SmartValidator, ApplicationContextAware,
		InitializingBean, DisposableBean {

	private final SpringValidatorAdapter target;

	private final boolean existingBean;

	WebMvcValidator(SpringValidatorAdapter target, boolean existingBean) {
		this.target = target;
		this.existingBean = existingBean;
	}

	SpringValidatorAdapter getTarget() {
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
	public void validate(Object target, Errors errors, Object... validationHints) {
		this.target.validate(target, errors, validationHints);
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		if (!this.existingBean && this.target instanceof ApplicationContextAware) {
			((ApplicationContextAware) this.target)
					.setApplicationContext(applicationContext);
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (!this.existingBean && this.target instanceof InitializingBean) {
			((InitializingBean) this.target).afterPropertiesSet();
		}
	}

	@Override
	public void destroy() throws Exception {
		if (!this.existingBean && this.target instanceof DisposableBean) {
			((DisposableBean) this.target).destroy();
		}
	}

	public static Validator get(ApplicationContext applicationContext,
			Validator validator) {
		if (validator != null) {
			return wrap(validator, false);
		}
		return getExistingOrCreate(applicationContext);
	}

	private static Validator getExistingOrCreate(ApplicationContext applicationContext) {
		Validator existing = getExisting(applicationContext);
		if (existing != null) {
			return wrap(existing, true);
		}
		return create();
	}

	private static Validator getExisting(ApplicationContext applicationContext) {
		try {
			javax.validation.Validator validator = applicationContext
					.getBean(javax.validation.Validator.class);
			if (validator instanceof Validator) {
				return (Validator) validator;
			}
			return new SpringValidatorAdapter(validator);
		}
		catch (NoSuchBeanDefinitionException ex) {
			return null;
		}
	}

	private static Validator create() {
		OptionalValidatorFactoryBean validator = new OptionalValidatorFactoryBean();
		validator.setMessageInterpolator(new MessageInterpolatorFactory().getObject());
		return wrap(validator, false);
	}

	private static Validator wrap(Validator validator, boolean existingBean) {
		if (validator instanceof javax.validation.Validator) {
			if (validator instanceof SpringValidatorAdapter) {
				return new WebMvcValidator((SpringValidatorAdapter) validator,
						existingBean);
			}
			return new WebMvcValidator(
					new SpringValidatorAdapter((javax.validation.Validator) validator),
					existingBean);
		}
		return validator;
	}

}
