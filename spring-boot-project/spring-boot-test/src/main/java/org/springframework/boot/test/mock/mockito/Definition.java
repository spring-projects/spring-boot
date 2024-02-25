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

package org.springframework.boot.test.mock.mockito;

import org.springframework.util.ObjectUtils;

/**
 * Base class for {@link MockDefinition} and {@link SpyDefinition}.
 *
 * @author Phillip Webb
 * @see DefinitionsParser
 */
abstract class Definition {

	private static final int MULTIPLIER = 31;

	private final String name;

	private final MockReset reset;

	private final boolean proxyTargetAware;

	private final QualifierDefinition qualifier;

	/**
     * Constructs a new Definition with the given parameters.
     *
     * @param name the name of the Definition
     * @param reset the reset behavior for the Definition, defaults to MockReset.AFTER if null
     * @param proxyTargetAware true if the Definition is proxy target aware, false otherwise
     * @param qualifier the qualifier definition for the Definition
     */
    Definition(String name, MockReset reset, boolean proxyTargetAware, QualifierDefinition qualifier) {
		this.name = name;
		this.reset = (reset != null) ? reset : MockReset.AFTER;
		this.proxyTargetAware = proxyTargetAware;
		this.qualifier = qualifier;
	}

	/**
	 * Return the name for bean.
	 * @return the name or {@code null}
	 */
	String getName() {
		return this.name;
	}

	/**
	 * Return the mock reset mode.
	 * @return the reset mode
	 */
	MockReset getReset() {
		return this.reset;
	}

	/**
	 * Return if AOP advised beans should be proxy target aware.
	 * @return if proxy target aware
	 */
	boolean isProxyTargetAware() {
		return this.proxyTargetAware;
	}

	/**
	 * Return the qualifier or {@code null}.
	 * @return the qualifier
	 */
	QualifierDefinition getQualifier() {
		return this.qualifier;
	}

	/**
     * Compares this Definition object to the specified object for equality.
     * Returns true if the specified object is also a Definition object and all
     * the corresponding fields have the same values. Otherwise, returns false.
     *
     * @param obj the object to compare this Definition against
     * @return true if the given object is equal to this Definition, false otherwise
     */
    @Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj == null || !getClass().isAssignableFrom(obj.getClass())) {
			return false;
		}
		Definition other = (Definition) obj;
		boolean result = true;
		result = result && ObjectUtils.nullSafeEquals(this.name, other.name);
		result = result && ObjectUtils.nullSafeEquals(this.reset, other.reset);
		result = result && ObjectUtils.nullSafeEquals(this.proxyTargetAware, other.proxyTargetAware);
		result = result && ObjectUtils.nullSafeEquals(this.qualifier, other.qualifier);
		return result;
	}

	/**
     * Returns a hash code value for the object. This method overrides the default implementation of the hashCode() method.
     * The hash code is calculated based on the values of the name, reset, proxyTargetAware, and qualifier properties.
     * 
     * @return the hash code value for the object
     */
    @Override
	public int hashCode() {
		int result = 1;
		result = MULTIPLIER * result + ObjectUtils.nullSafeHashCode(this.name);
		result = MULTIPLIER * result + ObjectUtils.nullSafeHashCode(this.reset);
		result = MULTIPLIER * result + ObjectUtils.nullSafeHashCode(this.proxyTargetAware);
		result = MULTIPLIER * result + ObjectUtils.nullSafeHashCode(this.qualifier);
		return result;
	}

}
