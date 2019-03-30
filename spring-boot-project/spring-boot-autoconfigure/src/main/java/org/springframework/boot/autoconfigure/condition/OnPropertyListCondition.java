/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.autoconfigure.condition;

import java.util.List;
import java.util.function.Supplier;

import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * {@link Condition} that checks if a property whose value is a list is defined in the
 * environment.
 *
 * @author Eneias Silva
 * @author Stephane Nicoll
 * @since 2.0.5
 */
public class OnPropertyListCondition extends SpringBootCondition {

	private static final Bindable<List<String>> STRING_LIST = Bindable
			.listOf(String.class);

	private final String propertyName;

	private final Supplier<ConditionMessage.Builder> messageBuilder;

	/**
	 * Create a new instance with the property to check and the message builder to use.
	 * @param propertyName the name of the property
	 * @param messageBuilder a message builder supplier that should provide a fresh
	 * instance on each call
	 */
	protected OnPropertyListCondition(String propertyName,
			Supplier<ConditionMessage.Builder> messageBuilder) {
		this.propertyName = propertyName;
		this.messageBuilder = messageBuilder;
	}

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context,
			AnnotatedTypeMetadata metadata) {
		BindResult<?> property = Binder.get(context.getEnvironment())
				.bind(this.propertyName, STRING_LIST);
		ConditionMessage.Builder messageBuilder = this.messageBuilder.get();
		if (property.isBound()) {
			return ConditionOutcome
					.match(messageBuilder.found("property").items(this.propertyName));
		}
		return ConditionOutcome
				.noMatch(messageBuilder.didNotFind("property").items(this.propertyName));
	}

}
