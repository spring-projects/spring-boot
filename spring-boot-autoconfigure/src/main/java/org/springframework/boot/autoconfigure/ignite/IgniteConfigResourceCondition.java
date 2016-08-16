/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.autoconfigure.ignite;

import org.apache.ignite.IgniteSystemProperties;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ResourceCondition;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * {@link SpringBootCondition} used to check if the Apache Ignite configuration is
 * available. This either kicks in if a default configuration has been found or if
 * configurable property referring to the resource to use has been set.
 *
 * @author wmz7year
 * @since 1.4.1
 */
public abstract class IgniteConfigResourceCondition extends ResourceCondition {

	private static final String CONFIG_SYSTEM_PROPERTY = IgniteSystemProperties.IGNITE_CONFIG_URL;

	protected IgniteConfigResourceCondition(String prefix, String propertyName) {
		super("Apache Ignite", prefix, propertyName, "file:./ignite.xml",
				"classpath:/ignite.xml");
	}

	@Override
	protected ConditionOutcome getResourceOutcome(ConditionContext context,
			AnnotatedTypeMetadata metadata) {
		if (System.getProperty(CONFIG_SYSTEM_PROPERTY) != null) {
			return ConditionOutcome
					.match("System property '" + CONFIG_SYSTEM_PROPERTY + "' is set.");
		}
		return super.getResourceOutcome(context, metadata);
	}
}
