/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.json.app;

/**
 * Example object to read/write as JSON via {@link ExampleJsonComponent}.
 *
 * @author Phillip Webb
 */
public class ExampleCustomObject {

	private String value;

	public ExampleCustomObject(String value) {
		this.value = value;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj != null && obj.getClass().equals(getClass())) {
			return this.value.equals(((ExampleCustomObject) obj).value);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.value.hashCode();
	}

	@Override
	public String toString() {
		return this.value;
	}

}
