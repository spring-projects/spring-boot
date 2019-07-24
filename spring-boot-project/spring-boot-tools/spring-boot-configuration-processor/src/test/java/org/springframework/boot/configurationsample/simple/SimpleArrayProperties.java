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

package org.springframework.boot.configurationsample.simple;

import java.util.Map;

import org.springframework.boot.configurationsample.ConfigurationProperties;

/**
 * Properties with array.
 *
 * @author Stephane Nicoll
 */
@ConfigurationProperties("array")
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

	public static class Holder {

	}

}
