/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.simple;

import java.beans.FeatureDescriptor;
import java.util.Comparator;

import org.springframework.boot.configurationsample.TestConfigurationProperties;

/**
 * Simple properties.
 *
 * @author Stephane Nicoll
 */
@TestConfigurationProperties("simple")
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
