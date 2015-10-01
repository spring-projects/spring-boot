/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.actuate.endpoint;

import java.util.Collection;

import org.springframework.boot.actuate.metrics.Metric;

/**
 * Interface to expose specific {@link Metric}s via a {@link MetricsEndpoint}.
 *
 * @author Dave Syer
 * @see SystemPublicMetrics
 */
public interface PublicMetrics {

	/**
	 * Return an indication of current state through metrics.
	 * @return the public metrics
	 */
	Collection<Metric<?>> metrics();

}
