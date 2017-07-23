/*
 * Copyright 2012-2016 the original author or authors.
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

import java.util.function.Consumer;

/**
 * Fast writes to in-memory metrics store using {@link GaugeBuffer}.
 *
 * @author Dave Syer
 * @since 1.3.0
 */
public class GaugeBuffers extends Buffers<GaugeBuffer> {

	public void set(final String name, final double value) {
		doWith(name, new Consumer<GaugeBuffer>() {
			@Override
			public void accept(GaugeBuffer buffer) {
				buffer.setTimestamp(System.currentTimeMillis());
				buffer.setValue(value);
			}
		});
	}

	@Override
	protected GaugeBuffer createBuffer() {
		return new GaugeBuffer(0L);
	}

}
