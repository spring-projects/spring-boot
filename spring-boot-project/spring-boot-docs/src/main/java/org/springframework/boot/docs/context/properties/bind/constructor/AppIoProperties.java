/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.docs.context.properties.bind.constructor;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.boot.convert.DataSizeUnit;
import org.springframework.util.unit.DataSize;
import org.springframework.util.unit.DataUnit;

/**
 * A {@link ConfigurationProperties @ConfigurationProperties} example that uses
 * {@link DataSize}.
 *
 * @author Stephane Nicoll
 */
// tag::example[]
@ConfigurationProperties("app.io")
@ConstructorBinding
public class AppIoProperties {

	private final DataSize bufferSize;

	private final DataSize sizeThreshold;

	public AppIoProperties(@DataSizeUnit(DataUnit.MEGABYTES) @DefaultValue("2MB") DataSize bufferSize,
			@DefaultValue("512B") DataSize sizeThreshold) {
		this.bufferSize = bufferSize;
		this.sizeThreshold = sizeThreshold;
	}

	public DataSize getBufferSize() {
		return this.bufferSize;
	}

	public DataSize getSizeThreshold() {
		return this.sizeThreshold;
	}

}
// end::example[]
