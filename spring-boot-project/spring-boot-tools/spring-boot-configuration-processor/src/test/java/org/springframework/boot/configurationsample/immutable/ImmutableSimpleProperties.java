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

package org.springframework.boot.configurationsample.immutable;

import java.util.Comparator;

import org.springframework.boot.configurationsample.ConfigurationProperties;
import org.springframework.boot.configurationsample.ConstructorBinding;
import org.springframework.boot.configurationsample.DefaultValue;

/**
 * Simple properties, in immutable format.
 *
 * @author Stephane Nicoll
 */
@ConfigurationProperties("immutable")
public class ImmutableSimpleProperties {

	/**
	 * The name of this simple properties.
	 */
	private final String theName;

	/**
	 * A simple flag.
	 */
	private final boolean flag;

	// An interface can still be injected because it might have a converter
	private final Comparator<?> comparator;

	// Even if it is not exposed, we're still offering a way to bind the value via the
	// constructor so it should be present in the metadata
	@SuppressWarnings("unused")
	private final Long counter;

	@ConstructorBinding
	public ImmutableSimpleProperties(@DefaultValue("boot") String theName, boolean flag, Comparator<?> comparator,
			Long counter) {
		this.theName = theName;
		this.flag = flag;
		this.comparator = comparator;
		this.counter = counter;
	}

	public String getTheName() {
		return this.theName;
	}

	@Deprecated
	public boolean isFlag() {
		return this.flag;
	}

	public Comparator<?> getComparator() {
		return this.comparator;
	}

}
