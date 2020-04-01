/*
 * Copyright 2012-2020 the original author or authors.
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

import com.hazelcast.core.HazelcastInstance;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionMessage.Builder;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.autoconfigure.hazelcast.HazelcastAutoConfiguration.HazelcastDataGridCondition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.Resource;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Hazelcast IMDG. Creates a
 * {@link HazelcastInstance} based on explicit configuration or when a default
 * configuration file is found in the environment.
 *
 * @author Stephane Nicoll
 * @author Vedran Pavic
 * @since 1.3.0
 * @see HazelcastConfigResourceCondition
 */
@Configuration(proxyBeanMethods = false)
@Conditional(HazelcastDataGridCondition.class)
@ConditionalOnClass(HazelcastInstance.class)
@EnableConfigurationProperties(HazelcastProperties.class)
@Import({ HazelcastClientConfiguration.class, HazelcastServerConfiguration.class })
public class HazelcastAutoConfiguration {

	static class HazelcastDataGridCondition extends SpringBootCondition {

		private static final String HAZELCAST_JET_CONFIG_FILE = "classpath:/hazelcast-jet-default.yaml";

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
			Builder message = ConditionMessage.forCondition(HazelcastDataGridCondition.class.getSimpleName());
			Resource resource = context.getResourceLoader().getResource(HAZELCAST_JET_CONFIG_FILE);
			if (resource.exists()) {
				return ConditionOutcome.noMatch(message.because("Found Hazelcast Jet on the classpath"));
			}
			return ConditionOutcome.match(message.because("Hazelcast Jet not found on the classpath"));
		}

	}

}
