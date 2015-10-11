/*
 * Copyright 2012-2015 the original author or authors.
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

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.bind.PropertySourcesPropertyValues;
import org.springframework.boot.bind.RelaxedDataBinder;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * {@link Condition} that checks whether or not the Spring resource handling chain is
 * enabled.
 *
 * @author Stephane Nicoll
 */
class OnEnabledResourceChainCondition extends SpringBootCondition {

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context,
			AnnotatedTypeMetadata metadata) {
		ConfigurableEnvironment environment = (ConfigurableEnvironment) context
				.getEnvironment();
		ResourceProperties properties = new ResourceProperties();
		RelaxedDataBinder binder = new RelaxedDataBinder(properties, "spring.resources");
		binder.bind(new PropertySourcesPropertyValues(environment.getPropertySources()));
		Boolean match = properties.getChain().getEnabled();
		return new ConditionOutcome(match,
				"Resource chain is " + (match ? "enabled" : "disabled"));
	}

}
