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

import org.springframework.boot.configurationsample.ConfigurationProperties;

/**
 * Configuration properties with various description styles.
 *
 * @author Stephane Nicoll
 */
@ConfigurationProperties("description")
public class DescriptionProperties {

	/**
	 * A simple description.
	 */
	private String simple;

	/**
	 * This is a lengthy description that spans across multiple lines to showcase that the
	 * line separators are cleaned automatically.
	 */
	private String multiLine;

	public String getSimple() {
		return this.simple;
	}

	public void setSimple(String simple) {
		this.simple = simple;
	}

	public String getMultiLine() {
		return this.multiLine;
	}

	public void setMultiLine(String multiLine) {
		this.multiLine = multiLine;
	}

}
