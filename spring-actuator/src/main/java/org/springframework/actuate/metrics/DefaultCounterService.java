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

package org.springframework.actuate.metrics;

import java.util.Date;

/**
 * Default implementation of {@link CounterService}.
 * 
 * @author Dave Syer
 */
public class DefaultCounterService implements CounterService {

	private MetricRepository repository;

	/**
	 * Create a {@link DefaultCounterService} instance.
	 * @param repository the underlying repository used to manage metrics
	 */
	public DefaultCounterService(MetricRepository repository) {
		super();
		this.repository = repository;
	}

	@Override
	public void increment(String metricName) {
		this.repository.increment(wrap(metricName), 1, new Date());
	}

	@Override
	public void decrement(String metricName) {
		this.repository.increment(wrap(metricName), -1, new Date());
	}

	@Override
	public void reset(String metricName) {
		this.repository.set(wrap(metricName), 0, new Date());
	}

	private String wrap(String metricName) {
		if (metricName.startsWith("counter")) {
			return metricName;
		}
		else {
			return "counter." + metricName;
		}
	}

}
