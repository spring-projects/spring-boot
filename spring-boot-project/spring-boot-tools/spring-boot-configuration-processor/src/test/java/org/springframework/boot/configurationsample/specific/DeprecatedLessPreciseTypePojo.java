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

package org.springframework.boot.configurationsample.specific;

import org.springframework.boot.configurationsample.ConfigurationProperties;

/**
 * Demonstrate that deprecating accessor with not the same type is not taken into account
 * to detect the deprecated flag.
 *
 * @author Stephane Nicoll
 */
@ConfigurationProperties("not.deprecated")
public class DeprecatedLessPreciseTypePojo {

	private boolean flag;

	@Deprecated
	public Boolean getFlag() {
		return this.flag;
	}

	public boolean isFlag() {
		return this.flag;
	}

	public void setFlag(boolean flag) {
		this.flag = flag;
	}

	@Deprecated
	public void setFlag(Boolean flag) {
		this.flag = flag;
	}

}
