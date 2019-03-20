/*
 * Copyright 2012-2014 the original author or authors.
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

import java.beans.FeatureDescriptor;
import java.util.Comparator;

import org.springframework.boot.configurationsample.ConfigurationProperties;

/**
 * Simple properties.
 *
 * @author Stephane Nicoll
 */
@ConfigurationProperties(prefix = "simple")
public class SimpleProperties {

	/**
	 * The name of this simple properties.
	 */
	private String theName = "boot";

	// isFlag is also detected
	/**
	 * A simple flag.
	 */
	private boolean flag;

	// An interface can still be injected because it might have a converter
	private Comparator<?> comparator;

	// There is only a getter on this instance but we don't know what to do with it ->
	// ignored
	private FeatureDescriptor featureDescriptor;

	// There is only a setter on this "simple" property --> ignored
	@SuppressWarnings("unused")
	private Long counter;

	// There is only a getter on this "simple" property --> ignored
	private Integer size;

	public String getTheName() {
		return this.theName;
	}

	@Deprecated
	public void setTheName(String name) {
		this.theName = name;
	}

	@Deprecated
	public boolean isFlag() {
		return this.flag;
	}

	public void setFlag(boolean flag) {
		this.flag = flag;
	}

	public Comparator<?> getComparator() {
		return this.comparator;
	}

	public void setComparator(Comparator<?> comparator) {
		this.comparator = comparator;
	}

	public FeatureDescriptor getFeatureDescriptor() {
		return this.featureDescriptor;
	}

	public void setCounter(Long counter) {
		this.counter = counter;
	}

	public Integer getSize() {
		return this.size;
	}

}
