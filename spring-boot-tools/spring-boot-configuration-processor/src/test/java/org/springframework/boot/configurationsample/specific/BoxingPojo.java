/*
 * Copyright 2012-2015 the original author or authors.
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
 * Demonstrate the use of boxing/unboxing. Even if the type does not strictly match, it
 * should still be detected.
 *
 * @author Stephane Nicoll
 */
@ConfigurationProperties("boxing")
public class BoxingPojo {

	private boolean flag;

	private Integer counter;

	public boolean isFlag() {
		return this.flag;
	}

	// Setter use Boolean
	public void setFlag(Boolean flag) {
		this.flag = flag;
	}

	public Integer getCounter() {
		return this.counter;
	}

	// Setter use int
	public void setCounter(int counter) {
		this.counter = counter;
	}

}
