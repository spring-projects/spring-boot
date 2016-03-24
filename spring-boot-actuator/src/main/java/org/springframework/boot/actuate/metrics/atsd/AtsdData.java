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

package org.springframework.boot.actuate.metrics.atsd;

/**
 * ATSD Data.
 *
 * @author Alexander Tokarev.
 */
public class AtsdData {
	private AtsdName atsdName;
	private Number value;
	private Long timestamp;

	public AtsdData(AtsdName atsdName, Number value, Long timestamp) {
		this.atsdName = atsdName;
		this.value = value;
		this.timestamp = timestamp;
	}

	public AtsdData(AtsdName atsdName, Number value) {
		this(atsdName, value, null);
	}

	public AtsdName getAtsdName() {
		return this.atsdName;
	}

	public Number getValue() {
		return this.value;
	}

	public Long getTimestamp() {
		return this.timestamp;
	}

	@Override
	public String toString() {
		return "AtsdData{" +
				"atsdName=" + this.atsdName +
				", value=" + this.value +
				", timestamp=" + this.timestamp +
				'}';
	}
}
