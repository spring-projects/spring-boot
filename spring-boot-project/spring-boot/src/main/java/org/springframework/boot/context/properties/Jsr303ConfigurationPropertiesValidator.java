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

package org.springframework.boot.context.properties;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.springframework.boot.validation.MessageInterpolatorFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * Validator that supports configuration classes annotated with
 * {@link Validated @Validated}.
 *
 * @author Phillip Webb
 */
class Jsr303ConfigurationPropertiesValidator implements Validator {

	private final Future<Delegate> delegate;

	Jsr303ConfigurationPropertiesValidator(ApplicationContext applicationContext) {
		// Creating the delegate is slow so we do it in the background
		ExecutorService executor = Executors.newSingleThreadExecutor();
		this.delegate = executor.submit(() -> new Delegate(applicationContext));
		executor.shutdown();
		if (applicationContext instanceof ConfigurableApplicationContext) {
			((ConfigurableApplicationContext) applicationContext)
					.addApplicationListener(new Listener());
		}
	}

	@Override
	public boolean supports(Class<?> type) {
		return AnnotatedElementUtils.hasAnnotation(type, Validated.class)
				&& getDelegate().supports(type);
	}

	@Override
	public void validate(Object target, Errors errors) {
		getDelegate().validate(target, errors);
	}

	private Delegate getDelegate() {
		try {
			return this.delegate.get();
		}
		catch (InterruptedException ex) {
			throw new IllegalStateException(ex);
		}
		catch (ExecutionException ex) {
			ReflectionUtils.rethrowRuntimeException(ex.getCause());
			return null;
		}
	}

	private static class Delegate extends LocalValidatorFactoryBean {

		Delegate(ApplicationContext applicationContext) {
			setApplicationContext(applicationContext);
			setMessageInterpolator(new MessageInterpolatorFactory().getObject());
			afterPropertiesSet();
		}

	}

	private class Listener implements ApplicationListener<ContextRefreshedEvent> {

		@Override
		public void onApplicationEvent(ContextRefreshedEvent event) {
			getDelegate();
		}

	}

}
