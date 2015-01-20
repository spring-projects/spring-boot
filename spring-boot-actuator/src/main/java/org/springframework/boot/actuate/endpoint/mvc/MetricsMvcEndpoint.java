/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.mvc;

import org.springframework.boot.actuate.endpoint.KeyUtils;
import org.springframework.boot.actuate.endpoint.MetricsEndpoint;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Adapter to expose {@link MetricsEndpoint} as an {@link MvcEndpoint}.
 *
 * @author Dave Syer
 * @author Sergei Egorov
 */
public class MetricsMvcEndpoint extends EndpointMvcAdapter {

	private final MetricsEndpoint delegate;

	public MetricsMvcEndpoint(MetricsEndpoint delegate) {
		super(delegate);
		this.delegate = delegate;
	}

	@RequestMapping(value = "/{regexp:.*}", method = RequestMethod.GET)
	@ResponseBody
	public Object value(@PathVariable String regexp) {
		Map<String, Object> metrics = this.delegate.invoke();
		Map<String, Object> result = new HashMap<String, Object>();

		Pattern keyPattern = KeyUtils.getPattern(regexp);
		for (Map.Entry<String, Object> metricEntry : metrics.entrySet()) {
			if(keyPattern.matcher(metricEntry.getKey()).matches()) {
				result.put(metricEntry.getKey(), metricEntry.getValue());
			}
		}

		return result;
	}
}
