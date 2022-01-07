/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.autoconfigure.sql.init;

import java.util.Locale;

import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.sql.init.DatabaseInitializationMode;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

/**
 * Condition that checks if the database initialization of a particular component should
 * be considered.
 *
 * @author Stephane Nicoll
 * @since 2.6.2
 * @see DatabaseInitializationMode
 */
public class OnDatabaseInitializationCondition extends SpringBootCondition {

	private final String name;

	private final String[] propertyNames;

	/**
	 * Create a new instance with the name of the component and the property names to
	 * check, in order. If a property is set, its value is used to determine the outcome
	 * and remaining properties are not tested.
	 * @param name the name of the component
	 * @param propertyNames the properties to check (in order)
	 */
	public OnDatabaseInitializationCondition(String name, String... propertyNames) {
		this.name = name;
		this.propertyNames = propertyNames;
	}

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		Environment environment = context.getEnvironment();
		String propertyName = getConfiguredProperty(environment);
		DatabaseInitializationMode mode = getDatabaseInitializationMode(environment, propertyName);
		boolean match = match(mode);
		String messagePrefix = (propertyName != null) ? propertyName : "default value";
		return new ConditionOutcome(match, ConditionMessage.forCondition(this.name + "Database Initialization")
				.because(messagePrefix + " is " + mode));
	}

	private boolean match(DatabaseInitializationMode mode) {
		return !mode.equals(DatabaseInitializationMode.NEVER);
	}

	private DatabaseInitializationMode getDatabaseInitializationMode(Environment environment, String propertyName) {
		if (StringUtils.hasText(propertyName)) {
			String candidate = environment.getProperty(propertyName, "embedded").toUpperCase(Locale.ENGLISH);
			if (StringUtils.hasText(candidate)) {
				return DatabaseInitializationMode.valueOf(candidate);
			}
		}
		return DatabaseInitializationMode.EMBEDDED;
	}

	private String getConfiguredProperty(Environment environment) {
		for (String propertyName : this.propertyNames) {
			if (environment.containsProperty(propertyName)) {
				return propertyName;
			}
		}
		return null;
	}

}
