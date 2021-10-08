/*
 * Copyright 2012-2021 the original author or authors.
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
import java.util.Arrays;
import java.util.List;

import org.springframework.boot.configurationsample.ConfigurationProperties;

/**
 * Simple properties including some with unresolvable default values.
 *
 * @author Chris Bono
 */
@ConfigurationProperties(prefix = "simple.bad.default")
public class SimpleBadDefaultProperties {

	/**
	 * The name of this simple properties.
	 */
	private String theName = "boot";

	/**
	 * A list of strings with unresolvable defaults.
	 */
	private List<String> someList = new ArrayList<>(Arrays.asList("a", "b", "c"));

	public String getTheName() {
		return theName;
	}

	public void setTheName(String theName) {
		this.theName = theName;
	}

	public List<String> getSomeList() {
		return someList;
	}

	public void setSomeList(List<String> someList) {
		this.someList = someList;
	}

}
