/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.simple;

import java.util.Map;

import org.springframework.boot.configurationsample.TestConfigurationProperties;

/**
 * Properties with array.
 *
 * @author Stephane Nicoll
 */
@TestConfigurationProperties("array")
public class SimpleArrayProperties {

	private int[] primitive;

	private String[] simple;

	private Holder[] inner;

	private Map<String, Integer>[] nameToInteger;

	public int[] getPrimitive() {
		return this.primitive;
	}

	public void setPrimitive(int[] primitive) {
		this.primitive = primitive;
	}

	public String[] getSimple() {
		return this.simple;
	}

	public void setSimple(String[] simple) {
		this.simple = simple;
	}

	public Holder[] getInner() {
		return this.inner;
	}

	public void setInner(Holder[] inner) {
		this.inner = inner;
	}

	public Map<String, Integer>[] getNameToInteger() {
		return this.nameToInteger;
	}

	public void setNameToInteger(Map<String, Integer>[] nameToInteger) {
		this.nameToInteger = nameToInteger;
	}

	static class Holder {

	}

}
