/*
 * Copyright 2010-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sample.data.geode.service.factory;

import org.springframework.util.Assert;

import sample.data.geode.service.Calculator;

/**
 * {@link CalculatorFactory} is an abstract factory class that constructs different instances
 * of the {@link Calculator} interface to compute the mathematical operations of an operand
 * applied to itself.
 *
 * In other words, given an operand (n), then f(n) = some calculation on (n).
 *
 * For example, addition(n) = n + n = 2n, multiplication(n) = n * n = n^2
 * and factorial(n) = n!, etc.
 *
 * @author John Blum
 * @see sample.data.geode.service.Calculator
 * @since 1.5.0
 */
@SuppressWarnings("unused")
public abstract class CalculatorFactory {

	/* (non-Javadoc) */
	public static Calculator<Long, Long> addition() {
		return new Calculator<Long, Long>() {
			@Override
			public Long calculate(Long operand) {
				Assert.notNull(operand, "operand must not be null");
				return (operand + operand);
			}

			@Override
			public String toString() {
				return "addition";
			}
		};
	}

	/* (non-Javadoc) */
	public static Calculator<Long, Long> factorial() {
		return new Calculator<Long, Long>() {
			@Override
			public Long calculate(Long operand) {
				Assert.notNull(operand, "operand must not be null");
				Assert.isTrue(operand > -1, String.format(
					"operand [%1$s] must be greater than equal to 0", operand));

				if (operand <= 2l) {
					return (operand < 2l ? 1l : 2l);
				}

				long result = operand;

				while (--operand > 1) {
					result *= operand;
				}

				return result;
			}

			@Override
			public String toString() {
				return "factorial";
			}
		};
	}

	/* (non-Javadoc) */
	public static Calculator<Long, Long> multiplication() {
		return new Calculator<Long, Long>() {
			@Override
			public Long calculate(Long operand) {
				Assert.notNull(operand, "operand must not be null");
				return (operand * operand);
			}

			@Override
			public String toString() {
				return "multiplication";
			}
		};
	}
}
