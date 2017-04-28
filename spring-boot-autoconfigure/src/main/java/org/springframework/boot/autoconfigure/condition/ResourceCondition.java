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

package org.springframework.boot.autoconfigure.condition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionMessage.Builder;
import org.springframework.boot.autoconfigure.condition.ConditionMessage.Style;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.io.Resource;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * {@link SpringBootCondition} used to check if a resource can be found using a
 * configurable property and optional default location(s).
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 1.3.0
 */
public abstract class ResourceCondition extends SpringBootCondition {

	private final String name;

	private final String property;

	private final String[] resourceLocations;

	/**
	 * Create a new condition.
	 * @param name the name of the component
	 * @param property the configuration property
	 * @param resourceLocations default location(s) where the configuration file can be
	 * found if the configuration key is not specified
	 * @since 2.0.0
	 */
	protected ResourceCondition(String name, String property,
			String... resourceLocations) {
		this.name = name;
		this.property = property;
		this.resourceLocations = resourceLocations;
	}

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context,
			AnnotatedTypeMetadata metadata) {
		if (context.getEnvironment().containsProperty(this.property)) {
			return ConditionOutcome.match(
					startConditionMessage().foundExactly("property " + this.property));
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
		List<String> found = new ArrayList<>();
		for (String location : this.resourceLocations) {
			Resource resource = context.getResourceLoader().getResource(location);
			if (resource != null && resource.exists()) {
				found.add(location);
			}
		}
		if (found.isEmpty()) {
			ConditionMessage message = startConditionMessage()
					.didNotFind("resource", "resources")
					.items(Style.QUOTE, Arrays.asList(this.resourceLocations));
			return ConditionOutcome.noMatch(message);
		}
		ConditionMessage message = startConditionMessage().found("resource", "resources")
				.items(Style.QUOTE, found);
		return ConditionOutcome.match(message);
	}

	protected final Builder startConditionMessage() {
		return ConditionMessage.forCondition("ResourceCondition", "(" + this.name + ")");
	}

}
