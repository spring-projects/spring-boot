/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.autoconfigure.context;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.DefaultLifecycleProcessor;

/**
 * {@link EnableAutoConfiguration Auto-configuration} relating to the application
 * context's lifecycle.
 *
 * @author Andy Wilkinson
 * @since 2.3.0
 */
@AutoConfiguration
@EnableConfigurationProperties(LifecycleProperties.class)
public class LifecycleAutoConfiguration {

	/**
     * Creates a default lifecycle processor bean if it is missing in the application context.
     * The lifecycle processor is responsible for managing the lifecycle of beans in the application.
     * If a custom lifecycle processor bean is already present in the context, this method will not create a new one.
     * The timeout per shutdown phase is set based on the provided properties.
     *
     * @param properties the lifecycle properties to configure the lifecycle processor
     * @return the default lifecycle processor bean
     */
    @Bean(name = AbstractApplicationContext.LIFECYCLE_PROCESSOR_BEAN_NAME)
	@ConditionalOnMissingBean(name = AbstractApplicationContext.LIFECYCLE_PROCESSOR_BEAN_NAME,
			search = SearchStrategy.CURRENT)
	public DefaultLifecycleProcessor defaultLifecycleProcessor(LifecycleProperties properties) {
		DefaultLifecycleProcessor lifecycleProcessor = new DefaultLifecycleProcessor();
		lifecycleProcessor.setTimeoutPerShutdownPhase(properties.getTimeoutPerShutdownPhase().toMillis());
		return lifecycleProcessor;
	}

}
