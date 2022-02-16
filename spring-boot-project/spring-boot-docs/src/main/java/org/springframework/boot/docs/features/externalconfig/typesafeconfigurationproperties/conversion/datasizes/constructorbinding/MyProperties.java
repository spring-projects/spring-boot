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

package org.springframework.boot.docs.features.externalconfig.typesafeconfigurationproperties.conversion.datasizes.constructorbinding;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.boot.convert.DataSizeUnit;
import org.springframework.util.unit.DataSize;
import org.springframework.util.unit.DataUnit;

@ConfigurationProperties("my")
public class MyProperties {

	// @fold:on // fields...
	private final DataSize bufferSize;

	private final DataSize sizeThreshold;

	// @fold:off
	public MyProperties(@DataSizeUnit(DataUnit.MEGABYTES) @DefaultValue("2MB") DataSize bufferSize,
			@DefaultValue("512B") DataSize sizeThreshold) {
		this.bufferSize = bufferSize;
		this.sizeThreshold = sizeThreshold;
	}

	// @fold:on // getters...
	public DataSize getBufferSize() {
		return this.bufferSize;
	}

	public DataSize getSizeThreshold() {
		return this.sizeThreshold;
	}
	// @fold:off

}
