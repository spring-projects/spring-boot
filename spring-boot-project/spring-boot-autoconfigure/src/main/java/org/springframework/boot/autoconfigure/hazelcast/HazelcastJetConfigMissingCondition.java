/*
 * Copyright 2012-2019 the original author or authors.
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
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.io.Resource;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * {@link SpringBootCondition} that checks if the Hazelcast Jet is missing on the
 * classpath by looking up the default configuration file.
 *
 * @author Ali Gurbuz
 * @since 2.3.0
 */
public class HazelcastJetConfigMissingCondition extends SpringBootCondition {

	private static final String HAZELCAST_JET_CONFIG_FILE = "classpath:/hazelcast-jet-default.yaml";

	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		Resource resource = context.getResourceLoader().getResource(HAZELCAST_JET_CONFIG_FILE);
		if (resource.exists()) {
			return ConditionOutcome.noMatch("Found Hazelcast Jet default config file on the classpath");
		}
		return ConditionOutcome.match("Hazelcast Jet default config file is missing on the classpath");
	}

}
