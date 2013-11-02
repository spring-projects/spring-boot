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
public class BootCreatedBeanInfo {

	private final String beanName;
	private final Class<?> beanType;
	private final List<String> decisions;

	public BootCreatedBeanInfo(String beanName, Object bean, List<String> decisions) {
		this.beanName = beanName;
		this.beanType = bean.getClass();
		this.decisions = decisions;
	}

	public BootCreatedBeanInfo(String beanName, Class<?> declaredBeanType, List<String> decisions) {
		this.beanName = beanName;
		this.beanType = declaredBeanType;
		this.decisions = decisions;
	}

	@Override
	public String toString() {
		return "BootCreatedBeanInfo{" + "beanName='" + beanName + '\'' + ", beanType=" + beanType
				+ ", decisions=" + decisions + '}';
	}

	public String getBeanName() {
		return beanName;
	}

	public Class<?> getBeanType() {
		return beanType;
	}

	public List<String> getDecisions() {
		return decisions;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		BootCreatedBeanInfo bootCreatedBeanInfo = (BootCreatedBeanInfo) o;

		if (beanName != null ? !beanName.equals(bootCreatedBeanInfo.beanName)
				: bootCreatedBeanInfo.beanName != null)
			return false;

		return true;
	}

	@Override
	public int hashCode() {
		return beanName != null ? beanName.hashCode() : 0;
	}
}
