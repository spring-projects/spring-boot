/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.configurationsample.arithmetic;

import org.springframework.boot.configurationsample.ConfigurationProperties;

/**
 * Configuration properties with arithmetic expressions to test metadata generation for
 * values that cannot be inferred at compile time. Used to verify that primitive types
 * with unknown default values omit the defaultValue field in the generated metadata.
 *
 * @author Hyeon Jae Kim
 */
@ConfigurationProperties(prefix = "arithmetic")
public class ArithmeticExpressionProperties {

	/**
	 * A value calculated using arithmetic expression that cannot be inferred.
	 */
	private int calculated = 10 * 10;

	/**
	 * A literal value that can be inferred.
	 */
	private int literal = 100;

	/**
	 * A boolean expression that cannot be inferred.
	 */
	private boolean complexFlag = !false;

	/**
	 * A simple boolean literal.
	 */
	private boolean simpleFlag = true;

	public int getCalculated() {
		return this.calculated;
	}

	public void setCalculated(int calculated) {
		this.calculated = calculated;
	}

	public int getLiteral() {
		return this.literal;
	}

	public void setLiteral(int literal) {
		this.literal = literal;
	}

	public boolean isComplexFlag() {
		return this.complexFlag;
	}

	public void setComplexFlag(boolean complexFlag) {
		this.complexFlag = complexFlag;
	}

	public boolean isSimpleFlag() {
		return this.simpleFlag;
	}

	public void setSimpleFlag(boolean simpleFlag) {
		this.simpleFlag = simpleFlag;
	}

}