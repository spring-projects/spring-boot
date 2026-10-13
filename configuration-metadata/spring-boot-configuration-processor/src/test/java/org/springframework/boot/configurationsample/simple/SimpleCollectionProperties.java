/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.simple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.springframework.boot.configurationsample.TestConfigurationProperties;

/**
 * Properties with collections.
 *
 * @author Stephane Nicoll
 */
@TestConfigurationProperties("collection")
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
