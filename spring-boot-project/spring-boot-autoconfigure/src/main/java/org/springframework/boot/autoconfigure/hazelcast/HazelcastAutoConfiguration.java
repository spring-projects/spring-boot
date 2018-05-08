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

package org.springframework.boot.autoconfigure.hazelcast;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.JetInstance;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Hazelcast. Creates a
 * {@link HazelcastInstance} and {@link JetInstance} based on explicit configuration
 * or when the relevant default configuration files are found in the environment.
 * <p>
 * Check {@link JetInstance} before {@link HazelcastInstance} as the former includes
 * the latter. Creating a {@link JetInstance} {@code @Bean} also exposes the
 * {@link HazelcastInstance} {@code @Bean}, which in turn de-activates
 * {@link HazelcastClientConfiguration} and {@link HazelcastServerConfiguration}.
 * </p>
 * <p>The name "<i>Hazelcast</i>" is usually used to mean Hazelcast's In-Memory
 * Data Grid (IMDG) rather than Hazelcast's Jet data streaming engine.
 * </p>
 *
 * @author Stephane Nicoll
 * @author Vedran Pavic
 * @author Neil Stevenson
 * @since 1.3.0
 * @see HazelcastConfigResourceCondition
 */
@Configuration
@ConditionalOnClass(HazelcastInstance.class)
@EnableConfigurationProperties(HazelcastProperties.class)
@Import({ HazelcastJetConfiguration.class, HazelcastClientConfiguration.class,
	HazelcastServerConfiguration.class })
public class HazelcastAutoConfiguration {

}
