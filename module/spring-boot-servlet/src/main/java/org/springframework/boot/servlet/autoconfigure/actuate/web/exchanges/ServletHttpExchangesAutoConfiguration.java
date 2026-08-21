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

package org.springframework.boot.servlet.autoconfigure.actuate.web.exchanges;

import org.springframework.boot.actuate.autoconfigure.web.exchanges.HttpExchangesProperties;
import org.springframework.boot.actuate.web.exchanges.HttpExchange;
import org.springframework.boot.actuate.web.exchanges.HttpExchangeRepository;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.servlet.actuate.web.exchanges.HttpExchangesFilter;
import org.springframework.boot.servlet.actuate.web.exchanges.HttpExchangesStartingFilter;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} to record {@link HttpExchange HTTP
 * exchanges}.
 *
 * <p>
 * Registers two filters that work together to ensure all requests — including those
 * rejected by high-priority filters such as Spring Security — are captured:
 * <ul>
 * <li>{@link HttpExchangesStartingFilter}: high-priority filter that starts recording at
 * the beginning of the filter chain.</li>
 * <li>{@link HttpExchangesFilter}: low-priority filter that finishes recording after all
 * other filters have run (capturing enriched headers).</li>
 * </ul>
 *
 * @author Dave Syer
 * @since 4.0.0
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnBooleanProperty(name = "management.httpexchanges.recording.enabled", matchIfMissing = true)
@ConditionalOnBean(HttpExchangeRepository.class)
@EnableConfigurationProperties(HttpExchangesProperties.class)
public final class ServletHttpExchangesAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	HttpExchangesFilter httpExchangesFilter(HttpExchangeRepository repository, HttpExchangesProperties properties) {
		return new HttpExchangesFilter(repository, properties.getRecording().getInclude());
	}

	@Bean
	@ConditionalOnMissingBean
	HttpExchangesStartingFilter httpExchangesStartingFilter(HttpExchangeRepository repository,
			HttpExchangesProperties properties) {
		return new HttpExchangesStartingFilter(repository, properties.getRecording().getInclude());
	}

}
