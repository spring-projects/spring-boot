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

package org.springframework.boot.actuate.metrics.buffer;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.boot.actuate.metrics.CounterService;
import org.springframework.lang.UsesJava8;

/**
 * Fast implementation of {@link CounterService} using {@link CounterBuffers}.
 *
 * @author Dave Syer
 * @since 1.3.0
 */
@UsesJava8
public class BufferCounterService implements CounterService {

	private final ConcurrentHashMap<String, String> names = new ConcurrentHashMap<String, String>();

	private final CounterBuffers writer;

	/**
	 * Create a {@link BufferCounterService} instance.
	 * @param writer the underlying writer used to manage metrics
	 */
	public BufferCounterService(CounterBuffers writer) {
		this.writer = writer;
	}

	@Override
	public void increment(String metricName) {
		this.writer.increment(wrap(metricName), 1L);
	}

	@Override
	public void decrement(String metricName) {
		this.writer.increment(wrap(metricName), -1L);
	}

	@Override
	public void reset(String metricName) {
		this.writer.reset(wrap(metricName));
	}

	private String wrap(String metricName) {
		if (this.names.containsKey(metricName)) {
			return this.names.get(metricName);
		}
		if (metricName.startsWith("counter") || metricName.startsWith("meter")) {
			return metricName;
		}
		String name = "counter." + metricName;
		this.names.put(metricName, name);
		return name;
	}

}
