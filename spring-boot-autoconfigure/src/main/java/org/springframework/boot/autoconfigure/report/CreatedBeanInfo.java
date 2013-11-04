/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.autoconfigure.report;

import java.util.List;

/**
 * A collection of data about a bean created by Boot
 * 
 * @author Greg Turnquist
 */
public class CreatedBeanInfo {

	private final String name;
	private final Class<?> type;
	private final List<String> decisions;

	public CreatedBeanInfo(String beanName, Class<?> declaredBeanType,
			List<String> decisions) {
		this.name = beanName;
		this.type = declaredBeanType;
		this.decisions = decisions;
	}

	@Override
	public String toString() {
		return "{" + "name='" + this.name + '\'' + ", type=" + this.type + ", decisions="
				+ this.decisions + '}';
	}

	public String getName() {
		return this.name;
	}

	public Class<?> getBeanType() {
		return this.type;
	}

	public List<String> getDecisions() {
		return this.decisions;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		CreatedBeanInfo bootCreatedBeanInfo = (CreatedBeanInfo) o;

		if (this.name != null ? !this.name.equals(bootCreatedBeanInfo.name)
				: bootCreatedBeanInfo.name != null)
			return false;

		return true;
	}

	@Override
	public int hashCode() {
		return this.name != null ? this.name.hashCode() : 0;
	}
}
