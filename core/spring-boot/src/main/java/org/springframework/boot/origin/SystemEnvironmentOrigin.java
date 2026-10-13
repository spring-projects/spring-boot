/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.origin;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * {@link Origin} for an item loaded from the system environment. Provides access to the
 * original property name.
 *
 * @author Madhura Bhave
 * @since 2.0.0
 */
public class SystemEnvironmentOrigin implements Origin {

	private final String property;

	public SystemEnvironmentOrigin(String property) {
		Assert.hasText(property, "'property' must not be empty");
		this.property = property;
	}

	public String getProperty() {
		return this.property;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		SystemEnvironmentOrigin other = (SystemEnvironmentOrigin) obj;
		return ObjectUtils.nullSafeEquals(this.property, other.property);
	}

	@Override
	public int hashCode() {
		return ObjectUtils.nullSafeHashCode(this.property);
	}

	@Override
	public String toString() {
		return "System Environment Property \"" + this.property + "\"";
	}

}
