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
