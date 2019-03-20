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

package org.springframework.boot.autoconfigure.couchbase;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.bind.PropertySourcesPropertyValues;
import org.springframework.boot.bind.RelaxedDataBinder;
import org.springframework.boot.bind.RelaxedNames;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySources;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.validation.DataBinder;

/**
 * Condition to determine if {@code spring.couchbase.bootstrap-hosts} is specified.
 *
 * @author Stephane Nicoll
 */
class OnBootstrapHostsCondition extends SpringBootCondition {

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context,
			AnnotatedTypeMetadata metadata) {
		Environment environment = context.getEnvironment();
		PropertyResolver resolver = new PropertyResolver(
				((ConfigurableEnvironment) environment).getPropertySources(),
				"spring.couchbase");
		Map.Entry<String, Object> entry = resolver.resolveProperty("bootstrap-hosts");
		if (entry != null) {
			return ConditionOutcome.match(ConditionMessage
					.forCondition(OnBootstrapHostsCondition.class.getName())
					.found("property").items("spring.couchbase.bootstrap-hosts"));
		}
		return ConditionOutcome.noMatch(ConditionMessage
				.forCondition(OnBootstrapHostsCondition.class.getName())
				.didNotFind("property").items("spring.couchbase.bootstrap-hosts"));
	}

	private static class PropertyResolver {

		private final String prefix;

		private final Map<String, Object> content;

		PropertyResolver(PropertySources propertySources, String prefix) {
			this.prefix = prefix;
			this.content = new HashMap<String, Object>();
			DataBinder binder = new RelaxedDataBinder(this.content, this.prefix);
			binder.bind(new PropertySourcesPropertyValues(propertySources));
		}

		Map.Entry<String, Object> resolveProperty(String name) {
			RelaxedNames prefixes = new RelaxedNames(this.prefix);
			RelaxedNames keys = new RelaxedNames(name);
			for (String prefix : prefixes) {
				for (String relaxedKey : keys) {
					String key = prefix + relaxedKey;
					if (this.content.containsKey(relaxedKey)) {
						return new AbstractMap.SimpleEntry<String, Object>(key,
								this.content.get(relaxedKey));
					}
				}
			}
			return null;
		}

	}

}
