/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sample.msgpack;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CalcResult {
	private final int left;
	private final int right;
	private final long result;

	@JsonCreator
	public CalcResult(@JsonProperty("left") int left, @JsonProperty("right") int right,
			@JsonProperty("result") long result) {
		this.left = left;
		this.right = right;
		this.result = result;
	}

	public int getLeft() {
		return left;
	}

	public int getRight() {
		return right;
	}

	public long getResult() {
		return result;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		CalcResult that = (CalcResult) o;

		if (left != that.left)
			return false;
		if (right != that.right)
			return false;
		return result == that.result;

	}

	@Override
	public int hashCode() {
		int result1 = left;
		result1 = 31 * result1 + right;
		result1 = 31 * result1 + (int) (result ^ (result >>> 32));
		return result1;
	}
}
