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

package org.springframework.boot.jackson;

import org.springframework.util.ObjectUtils;

/**
 * Sample object used for tests.
 *
 * @author Phillip Webb
 * @author Paul Aly
 */
public final class NameAndAge extends Name {

	private final int age;

	public NameAndAge(String name, int age) {
		super(name);
		this.age = age;
	}

	public int getAge() {
		return this.age;
	}

	public String asKey() {
		return this.name + " is " + this.age;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (obj instanceof NameAndAge) {
			NameAndAge other = (NameAndAge) obj;
			boolean rtn = true;
			rtn = rtn && ObjectUtils.nullSafeEquals(this.name, other.name);
			rtn = rtn && ObjectUtils.nullSafeEquals(this.age, other.age);
			return rtn;
		}
		return super.equals(obj);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ObjectUtils.nullSafeHashCode(this.name);
		result = prime * result + ObjectUtils.nullSafeHashCode(this.age);
		return result;
	}

}
