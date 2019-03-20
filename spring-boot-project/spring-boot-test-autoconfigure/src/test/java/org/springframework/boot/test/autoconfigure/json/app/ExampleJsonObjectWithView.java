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

import com.fasterxml.jackson.annotation.JsonView;

import org.springframework.util.ObjectUtils;

/**
 * Example object to read/write as JSON with view
 *
 * @author Madhura Bhave
 */
public class ExampleJsonObjectWithView {

	@JsonView(TestView.class)
	private String value;

	private int id;

	public String getValue() {
		return this.value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public int getId() {
		return this.id;
	}

	public void setId(int id) {
		this.id = id;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || obj.getClass() != getClass()) {
			return false;
		}
		ExampleJsonObjectWithView other = (ExampleJsonObjectWithView) obj;
		return ObjectUtils.nullSafeEquals(this.value, other.value)
				&& ObjectUtils.nullSafeEquals(this.id, other.id);
	}

	@Override
	public int hashCode() {
		return 0;
	}

	@Override
	public String toString() {
		return this.value + " " + this.id;
	}

	public static class TestView {

	}

}
