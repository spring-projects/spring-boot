/*
 * Copyright 2012-2014 the original author or authors.
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

import javax.naming.NamingException;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.jndi.JndiLocatorDelegate;
import org.springframework.jndi.JndiLocatorSupport;
import org.springframework.util.StringUtils;

/**
 * {@link Condition} that checks for JNDI locations.
 *
 * @author Phillip Webb
 * @since 1.2.0
 * @see ConditionalOnJndi
 */
@Order(Ordered.LOWEST_PRECEDENCE - 20)
class OnJndiCondition extends SpringBootCondition {

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context,
			AnnotatedTypeMetadata metadata) {
		AnnotationAttributes annotationAttributes = AnnotationAttributes.fromMap(metadata
				.getAnnotationAttributes(ConditionalOnJndi.class.getName()));
		String[] locations = annotationAttributes.getStringArray("value");
		try {
			return getMatchOutcome(locations);
		}
		catch (NoClassDefFoundError ex) {
			return ConditionOutcome.noMatch("JNDI class not found");
		}
	}

	private ConditionOutcome getMatchOutcome(String[] locations) {
		if (!isJndiAvailable()) {
			return ConditionOutcome.noMatch("JNDI environment is not available");
		}
		if (locations.length == 0) {
			return ConditionOutcome.match("JNDI environment is available");
		}
		JndiLocator locator = getJndiLocator(locations);
		String location = locator.lookupFirstLocation();
		if (location != null) {
			return ConditionOutcome.match("JNDI location '" + location
					+ "' found from candidates "
					+ StringUtils.arrayToCommaDelimitedString(locations));
		}
		return ConditionOutcome.noMatch("No JNDI location found from candidates "
				+ StringUtils.arrayToCommaDelimitedString(locations));
	}

	protected boolean isJndiAvailable() {
		return JndiLocatorDelegate.isDefaultJndiEnvironmentAvailable();
	}

	protected JndiLocator getJndiLocator(String[] locations) {
		return new JndiLocator(locations);
	}

	protected static class JndiLocator extends JndiLocatorSupport {

		private String[] locations;

		public JndiLocator(String[] locations) {
			this.locations = locations;
		}

		public String lookupFirstLocation() {
			for (String location : this.locations) {
				try {
					lookup(location);
					return location;
				}
				catch (NamingException ex) {
					// Swallow and continue
				}
			}
			return null;
		}

	}

}
