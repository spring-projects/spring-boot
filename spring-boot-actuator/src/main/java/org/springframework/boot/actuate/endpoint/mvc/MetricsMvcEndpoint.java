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

package org.springframework.boot.actuate.endpoint.mvc;

import java.util.Map;

import org.springframework.boot.actuate.endpoint.MetricsEndpoint;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Adapter to expose {@link MetricsEndpoint} as an {@link MvcEndpoint}.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Sergei Egorov
 */
public class MetricsMvcEndpoint extends EndpointMvcAdapter {

	private final MetricsEndpoint delegate;

	public MetricsMvcEndpoint(MetricsEndpoint delegate) {
		super(delegate);
		this.delegate = delegate;
	}

	@RequestMapping(value = "/{name:.*}", method = RequestMethod.GET)
	@ResponseBody
	public Object value(@PathVariable String name) {
		if (!this.delegate.isEnabled()) {
			// Shouldn't happen - MVC endpoint shouldn't be registered when delegate's
			// disabled
			return getDisabledResponse();
		}
		return new NamePatternMapFilter(this.delegate.invoke()).getResults(name);
	}

	/**
	 * {@link NamePatternFilter} for the Map source.
	 */
	private class NamePatternMapFilter extends NamePatternFilter<Map<String, Object>> {

		public NamePatternMapFilter(Map<String, Object> source) {
			super(source);
		}

		@Override
		protected void getNames(Map<String, Object> source, NameCallback callback) {
			for (String name : source.keySet()) {
				callback.addName(name);
			}
		}

		@Override
		protected Object getValue(Map<String, Object> source, String name) {
			Object value = source.get(name);
			if (value == null) {
				throw new NoSuchMetricException("No such metric: " + name);
			}
			return value;
		}

	}

	/**
	 * Exception thrown when the specified metric cannot be found.
	 */
	@SuppressWarnings("serial")
	@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "No such metric")
	public static class NoSuchMetricException extends RuntimeException {

		public NoSuchMetricException(String string) {
			super(string);
		}

	}

}
