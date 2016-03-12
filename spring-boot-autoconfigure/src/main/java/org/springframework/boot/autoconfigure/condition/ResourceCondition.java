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

package org.springframework.boot.autoconfigure.condition;

import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.io.Resource;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * {@link SpringBootCondition} used to check if a resource can be found using a
 * configurable property and optional default location(s).
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 1.3.0
 */
public abstract class ResourceCondition extends SpringBootCondition {

	private final String name;

	private final String prefix;

	private final String propertyName;

	private final String[] resourceLocations;

	/**
	 * Create a new condition.
	 * @param name the name of the component
	 * @param prefix the prefix of the configuration key
	 * @param propertyName the name of the configuration key
	 * @param resourceLocations default location(s) where the configuration file can be
	 * found if the configuration key is not specified
	 */
	protected ResourceCondition(String name, String prefix, String propertyName,
			String... resourceLocations) {
		this.name = name;
		this.prefix = (prefix.endsWith(".") ? prefix : prefix + ".");
		this.propertyName = propertyName;
		this.resourceLocations = resourceLocations;
	}

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context,
			AnnotatedTypeMetadata metadata) {
		RelaxedPropertyResolver resolver = new RelaxedPropertyResolver(
				context.getEnvironment(), this.prefix);
		if (resolver.containsProperty(this.propertyName)) {
			return ConditionOutcome.match("A '" + this.prefix + this.propertyName + "' "
					+ "property is specified");
		}
		return getResourceOutcome(context, metadata);
	}

	/**
	 * Check if one of the default resource locations actually exists.
	 * @param context the condition context
	 * @param metadata the annotation metadata
	 * @return the condition outcome
	 */
	protected ConditionOutcome getResourceOutcome(ConditionContext context,
			AnnotatedTypeMetadata metadata) {
		for (String location : this.resourceLocations) {
			Resource resource = context.getResourceLoader().getResource(location);
			if (resource != null && resource.exists()) {
				return ConditionOutcome
						.match("Found " + this.name + " config in " + resource);
			}
		}
		return ConditionOutcome
				.noMatch("No specific " + this.name + " configuration found");
	}

}
