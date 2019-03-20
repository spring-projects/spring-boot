/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.configurationsample.simple;

import org.springframework.boot.configurationsample.ConfigurationProperties;

/**
 * Class with nested configuration properties.
 *
 * @author Hrishikesh Joshi
 */
public class ClassWithNestedProperties {

	public static class NestedParentClass {

		private int parentClassProperty = 10;

		public int getParentClassProperty() {
			return this.parentClassProperty;
		}

		public void setParentClassProperty(int parentClassProperty) {
			this.parentClassProperty = parentClassProperty;
		}

	}

	@ConfigurationProperties(prefix = "nestedChildProps")
	public static class NestedChildClass extends NestedParentClass {

		private int childClassProperty = 20;

		public int getChildClassProperty() {
			return this.childClassProperty;
		}

		public void setChildClassProperty(int childClassProperty) {
			this.childClassProperty = childClassProperty;
		}

	}

}
