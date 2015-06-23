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

package org.springframework.boot.autoconfigure.cache;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.io.Resource;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * {@link SpringBootCondition} used to check if a cache configuration file can be found.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
abstract class CacheConfigFileCondition extends SpringBootCondition {

	private final String name;

	private final String prefix;

	private final String[] resourceLocations;

	public CacheConfigFileCondition(String name, String prefix,
			String... resourceLocations) {
		this.name = name;
		this.prefix = (prefix.endsWith(".") ? prefix : prefix + ".");
		this.resourceLocations = resourceLocations;
	}

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context,
			AnnotatedTypeMetadata metadata) {
		RelaxedPropertyResolver resolver = new RelaxedPropertyResolver(
				context.getEnvironment(), this.prefix);
		if (resolver.containsProperty("config")) {
			return ConditionOutcome.match("A '" + this.prefix + ".config' "
					+ "property is specified");
		}
		return getResourceOutcome(context, metadata);
	}

	protected ConditionOutcome getResourceOutcome(ConditionContext context,
			AnnotatedTypeMetadata metadata) {
		for (String location : this.resourceLocations) {
			Resource resource = context.getResourceLoader().getResource(location);
			if (resource != null && resource.exists()) {
				return ConditionOutcome.match("Found " + this.name + " config in "
						+ resource);
			}
		}
		return ConditionOutcome.noMatch("No specific " + this.name
				+ " configuration found");
	}

}
