/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.autoconfigure.reactor;

import reactor.core.publisher.Hooks;

import org.springframework.boot.LazyInitializationExcludeFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Reactor.
 *
 * @author Brian Clozel
 * @since 3.2.0
 */
@AutoConfiguration
@ConditionalOnClass(Hooks.class)
@EnableConfigurationProperties(ReactorProperties.class)
public class ReactorAutoConfiguration {

	ReactorAutoConfiguration(ReactorProperties properties) {
		if (properties.getContextPropagation() == ReactorProperties.ContextPropagationMode.AUTO) {
			Hooks.enableAutomaticContextPropagation();
		}
	}

	@Bean
	static LazyInitializationExcludeFilter reactorAutoConfigurationLazyInitializationExcludeFilter() {
		return LazyInitializationExcludeFilter.forBeanTypes(ReactorAutoConfiguration.class);
	}

}
