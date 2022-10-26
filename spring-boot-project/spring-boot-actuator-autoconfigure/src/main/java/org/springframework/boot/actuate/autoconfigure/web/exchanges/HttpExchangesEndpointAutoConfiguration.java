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

package org.springframework.boot.actuate.autoconfigure.web.exchanges;

import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.web.exchanges.HttpExchangeRepository;
import org.springframework.boot.actuate.web.exchanges.HttpExchangesEndpoint;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for the
 * {@link HttpExchangesEndpoint}.
 *
 * @author Phillip Webb
 * @since 3.0.0
 */
@AutoConfiguration(after = HttpExchangesAutoConfiguration.class)
@ConditionalOnAvailableEndpoint(endpoint = HttpExchangesEndpoint.class)
public class HttpExchangesEndpointAutoConfiguration {

	@Bean
	@ConditionalOnBean(HttpExchangeRepository.class)
	@ConditionalOnMissingBean
	public HttpExchangesEndpoint httpExchangesEndpoint(HttpExchangeRepository exchangeRepository) {
		return new HttpExchangesEndpoint(exchangeRepository);
	}

}
