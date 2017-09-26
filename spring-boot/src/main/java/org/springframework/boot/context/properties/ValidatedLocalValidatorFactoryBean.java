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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.validation.MessageInterpolatorFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * {@link LocalValidatorFactoryBean} supports classes annotated with
 * {@link Validated @Validated}.
 *
 * @author Phillip Webb
 */
class ValidatedLocalValidatorFactoryBean extends LocalValidatorFactoryBean
		implements ConfigurationPropertiesBinder.InternalValidator {

	private static final Log logger = LogFactory
			.getLog(ConfigurationPropertiesBindingPostProcessor.class);

	ValidatedLocalValidatorFactoryBean(ApplicationContext applicationContext) {
		setApplicationContext(applicationContext);
		setMessageInterpolator(new MessageInterpolatorFactory().getObject());
		afterPropertiesSet();
	}

	@Override
	public boolean supports(Class<?> type) {
		if (!super.supports(type)) {
			return false;
		}
		if (AnnotatedElementUtils.hasAnnotation(type, Validated.class)) {
			return true;
		}
		if (type.getPackage() != null
				&& type.getPackage().getName().startsWith("org.springframework.boot")) {
			return false;
		}
		if (getConstraintsForClass(type).isBeanConstrained()) {
			logger.warn("The @ConfigurationProperties bean " + type
					+ " contains validation constraints but had not been annotated "
					+ "with @Validated.");
		}
		return true;
	}

}
