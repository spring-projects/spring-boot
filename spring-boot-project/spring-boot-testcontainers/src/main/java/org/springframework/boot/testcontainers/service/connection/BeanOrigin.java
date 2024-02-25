/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.testcontainers.service.connection;

import java.util.Objects;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.origin.Origin;

/**
 * {@link Origin} backed by a Spring Bean.
 *
 * @author Phillip Webb
 */
class BeanOrigin implements Origin {

	private final String beanName;

	private final String resourceDescription;

	/**
     * Constructs a new BeanOrigin object with the specified bean name and bean definition.
     * 
     * @param beanName the name of the bean
     * @param beanDefinition the definition of the bean
     */
    BeanOrigin(String beanName, BeanDefinition beanDefinition) {
		this.beanName = beanName;
		this.resourceDescription = (beanDefinition != null) ? beanDefinition.getResourceDescription() : null;
	}

	/**
     * Compares this BeanOrigin object to the specified object for equality.
     * 
     * @param obj the object to compare to
     * @return true if the specified object is equal to this BeanOrigin object, false otherwise
     */
    @Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		BeanOrigin other = (BeanOrigin) obj;
		return Objects.equals(this.beanName, other.beanName);
	}

	/**
     * Returns the hash code value for this BeanOrigin object.
     * 
     * @return the hash code value for this object
     */
    @Override
	public int hashCode() {
		return this.beanName.hashCode();
	}

	/**
     * Returns a string representation of the BeanOrigin object.
     * 
     * @return a string representation of the BeanOrigin object
     */
    @Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append("Bean '");
		result.append(this.beanName);
		result.append("'");
		if (this.resourceDescription != null) {
			result.append(" defined in ");
			result.append(this.resourceDescription);
		}
		return result.toString();
	}

}
