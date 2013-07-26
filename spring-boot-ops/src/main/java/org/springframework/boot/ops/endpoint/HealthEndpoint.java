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

package org.springframework.boot.ops.endpoint;

import org.springframework.boot.ops.health.HealthIndicator;
import org.springframework.boot.strap.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

/**
 * {@link Endpoint} to expose application health.
 * 
 * @author Dave Syer
 */
@ConfigurationProperties(name = "endpoints.health", ignoreUnknownFields = false)
public class HealthEndpoint<T> extends AbstractEndpoint<T> {

	private HealthIndicator<? extends T> indicator;

	/**
	 * Create a new {@link HealthIndicator} instance.
	 * 
	 * @param indicator the health indicator
	 */
	public HealthEndpoint(HealthIndicator<? extends T> indicator) {
		super("/health", false);
		Assert.notNull(indicator, "Indicator must not be null");
		this.indicator = indicator;
	}

	@Override
	public T invoke() {
		return this.indicator.health();
	}

}
