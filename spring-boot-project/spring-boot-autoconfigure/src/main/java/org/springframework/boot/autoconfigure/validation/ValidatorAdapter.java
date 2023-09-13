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

package org.springframework.boot.autoconfigure.validation;

import jakarta.validation.ValidationException;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.validation.MessageInterpolatorFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.MessageSource;
import org.springframework.validation.Errors;
import org.springframework.validation.SmartValidator;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.OptionalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;

/**
 * {@link Validator} implementation that delegates calls to another {@link Validator}.
 * This {@link Validator} implements Spring's {@link SmartValidator} interface but does
 * not implement the JSR-303 {@code jakarta.validator.Validator} interface.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Zisis Pavloudis
 * @since 2.0.0
 */
public class ValidatorAdapter implements SmartValidator, ApplicationContextAware, InitializingBean, DisposableBean {

	private final SmartValidator target;

	private final boolean existingBean;

	ValidatorAdapter(SmartValidator target, boolean existingBean) {
		this.target = target;
		this.existingBean = existingBean;
	}

	public final Validator getTarget() {
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
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		if (!this.existingBean && this.target instanceof ApplicationContextAware contextAwareTarget) {
			contextAwareTarget.setApplicationContext(applicationContext);
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (!this.existingBean && this.target instanceof InitializingBean initializingBean) {
			initializingBean.afterPropertiesSet();
		}
	}

	@Override
	public void destroy() throws Exception {
		if (!this.existingBean && this.target instanceof DisposableBean disposableBean) {
			disposableBean.destroy();
		}
	}

	/**
	 * Return a {@link Validator} that only implements the {@link Validator} interface,
	 * wrapping it if necessary.
	 * <p>
	 * If the specified {@link Validator} is not {@code null}, it is wrapped. If not, a
	 * {@link jakarta.validation.Validator} is retrieved from the context and wrapped.
	 * Otherwise, a new default validator is created.
	 * @param applicationContext the application context
	 * @param validator an existing validator to use or {@code null}
	 * @return the validator to use
	 */
	public static Validator get(ApplicationContext applicationContext, Validator validator) {
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
		return create(applicationContext);
	}

	private static Validator getExisting(ApplicationContext applicationContext) {
		try {
			jakarta.validation.Validator validatorBean = applicationContext.getBean(jakarta.validation.Validator.class);
			if (validatorBean instanceof Validator validator) {
				return validator;
			}
			return new SpringValidatorAdapter(validatorBean);
		}
		catch (NoSuchBeanDefinitionException ex) {
			return null;
		}
	}

	private static Validator create(MessageSource messageSource) {
		OptionalValidatorFactoryBean validator = new OptionalValidatorFactoryBean();
		try {
			MessageInterpolatorFactory factory = new MessageInterpolatorFactory(messageSource);
			validator.setMessageInterpolator(factory.getObject());
		}
		catch (ValidationException ex) {
			// Ignore
		}
		return wrap(validator, false);
	}

	private static Validator wrap(Validator validator, boolean existingBean) {
		if (validator instanceof jakarta.validation.Validator jakartaValidator) {
			if (jakartaValidator instanceof SpringValidatorAdapter adapter) {
				return new ValidatorAdapter(adapter, existingBean);
			}
			return new ValidatorAdapter(new SpringValidatorAdapter(jakartaValidator), existingBean);
		}
		return validator;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> type) {
		if (type.isInstance(this.target)) {
			return (T) this.target;
		}
		return this.target.unwrap(type);
	}

}
