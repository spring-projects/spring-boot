/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.autoconfigure.hazelcast;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ResourceCondition;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * {@link SpringBootCondition} used to check if the Hazelcast configuration is available.
 * This either kicks in if a default configuration has been found or if configurable
 * property referring to the resource to use has been set.
 *
 * @author Stephane Nicoll
 * @since 1.3.0
 */
public abstract class HazelcastConfigResourceCondition extends ResourceCondition {

	static final String CONFIG_SYSTEM_PROPERTY = "hazelcast.config";

	protected HazelcastConfigResourceCondition(String prefix, String propertyName) {
		super("Hazelcast", prefix, propertyName, "file:./hazelcast.xml",
				"classpath:/hazelcast.xml");
	}

	@Override
	protected ConditionOutcome getResourceOutcome(ConditionContext context,
			AnnotatedTypeMetadata metadata) {
		if (System.getProperty(CONFIG_SYSTEM_PROPERTY) != null) {
			return ConditionOutcome.match(startConditionMessage()
					.because("System property '" + CONFIG_SYSTEM_PROPERTY + "' is set."));
		}
		return super.getResourceOutcome(context, metadata);
	}

}
