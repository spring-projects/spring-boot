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

package org.springframework.boot.autoconfigure.validation;

import javax.validation.Validator;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.validation.SmartValidator;

/**
 * JSR 303 adapter configration imported by {@link ValidationAutoConfiguration}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
@Configuration
class Jsr303ValidatorAdapterConfiguration {

	@Bean
	@ConditionalOnSingleCandidate(Validator.class)
	@ConditionalOnMissingBean(org.springframework.validation.Validator.class)
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public SmartValidator jsr303ValidatorAdapter(Validator validator) {
		return new DelegatingValidator(validator);
	}

}
