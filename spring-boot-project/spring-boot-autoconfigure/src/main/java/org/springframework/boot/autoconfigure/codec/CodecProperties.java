/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigure.codec;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

/**
 * {@link ConfigurationProperties properties} for reactive codecs.
 *
 * @author Brian Clozel
 * @since 2.2.1
 */
@ConfigurationProperties(prefix = "spring.codec")
public class CodecProperties {

	/**
	 * Limit on the number of bytes that can be buffered whenever the input stream needs
	 * to be aggregated. By default this is not set, in which case individual codec
	 * defaults apply. Most codecs are limited to 256K by default.
	 */
	private DataSize maxInMemorySize;

	public DataSize getMaxInMemorySize() {
		return this.maxInMemorySize;
	}

	public void setMaxInMemorySize(DataSize maxInMemorySize) {
		this.maxInMemorySize = maxInMemorySize;
	}

}
