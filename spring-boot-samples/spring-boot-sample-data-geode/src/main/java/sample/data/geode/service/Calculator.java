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

package sample.data.geode.service;

/**
 * The {@link Calculator} interface represents a mathematical computation
 * of an operand with itself.
 *
 * @author John Blum
 * @since 1.5.0
 */
public interface Calculator<S, T> {

	/**
	 * Calculates the value of the operand with itself.
	 *
	 * @param operand the operand to used in the calculation.
	 * @return the result of the operand calculated with itself.
	 */
	T calculate(S operand);

}
