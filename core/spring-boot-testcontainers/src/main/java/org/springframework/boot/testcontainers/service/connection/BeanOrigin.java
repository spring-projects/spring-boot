/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testcontainers.service.connection;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.origin.Origin;

/**
 * {@link Origin} backed by a Spring Bean.
 *
 * @author Phillip Webb
 */
class BeanOrigin implements Origin {

	private final String beanName;

	private final @Nullable String resourceDescription;

	BeanOrigin(String beanName, @Nullable BeanDefinition beanDefinition) {
		this.beanName = beanName;
		this.resourceDescription = (beanDefinition != null) ? beanDefinition.getResourceDescription() : null;
	}

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

	@Override
	public int hashCode() {
		return this.beanName.hashCode();
	}

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
