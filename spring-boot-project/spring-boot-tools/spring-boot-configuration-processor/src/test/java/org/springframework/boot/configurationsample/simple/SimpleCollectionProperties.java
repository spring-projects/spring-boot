/*
 * Copyright 2012-2018 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.springframework.boot.configurationsample.ConfigurationProperties;

/**
 * Properties with collections.
 *
 * @author Stephane Nicoll
 */
@ConfigurationProperties(prefix = "collection")
public class SimpleCollectionProperties {

	private Map<Integer, String> integersToNames;

	private Collection<Long> longs;

	private List<Float> floats;

	private final Map<String, Integer> namesToIntegers = new HashMap<>();

	private final Collection<Byte> bytes = new LinkedHashSet<>();

	private final List<Double> doubles = new ArrayList<>();

	private final Map<String, Holder<String>> namesToHolders = new HashMap<>();

	public Map<Integer, String> getIntegersToNames() {
		return this.integersToNames;
	}

	public void setIntegersToNames(Map<Integer, String> integersToNames) {
		this.integersToNames = integersToNames;
	}

	public Collection<Long> getLongs() {
		return this.longs;
	}

	public void setLongs(Collection<Long> longs) {
		this.longs = longs;
	}

	public List<Float> getFloats() {
		return this.floats;
	}

	public void setFloats(List<Float> floats) {
		this.floats = floats;
	}

	public Map<String, Integer> getNamesToIntegers() {
		return this.namesToIntegers;
	}

	public Collection<Byte> getBytes() {
		return this.bytes;
	}

	public List<Double> getDoubles() {
		return this.doubles;
	}

	public Map<String, Holder<String>> getNamesToHolders() {
		return this.namesToHolders;
	}

	public static class Holder<T> {

		@SuppressWarnings("unused")
		private T target;

		public void setTarget(T target) {
			this.target = target;
		}

	}

}
