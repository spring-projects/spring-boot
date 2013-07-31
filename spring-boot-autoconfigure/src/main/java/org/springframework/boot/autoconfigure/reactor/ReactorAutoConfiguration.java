/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.autoconfigure.reactor;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import reactor.core.Environment;
import reactor.core.Reactor;
import reactor.spring.context.ConsumerBeanPostProcessor;

/**
 * @author Dave Syer
 */
@Configuration
@ConditionalOnClass(ConsumerBeanPostProcessor.class)
@ConditionalOnMissingBean(Reactor.class)
@AutoConfigureAfter(WebMvcAutoConfiguration.class)
public class ReactorAutoConfiguration {

	@Bean
	public Environment reactorEnvironment() {
		return new Environment(); // TODO: use Spring Environment to configure?
	}

	@Bean
	public Reactor rootReactor() {
		return reactorEnvironment().getRootReactor();
	}

	@Bean
	@Order(Ordered.LOWEST_PRECEDENCE)
	protected ConsumerBeanPostProcessor reactorConsumerBeanPostProcessor() {
		return new ConsumerBeanPostProcessor();
	}

}
