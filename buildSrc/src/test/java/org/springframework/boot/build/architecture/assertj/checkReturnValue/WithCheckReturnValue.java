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

package org.springframework.boot.build.architecture.assertj.checkReturnValue;

import org.assertj.core.api.AbstractAssert;

import org.springframework.lang.CheckReturnValue;

public class WithCheckReturnValue extends AbstractAssert<WithCheckReturnValue, Object> {

	WithCheckReturnValue() {
		super(null, WithCheckReturnValue.class);
	}

	@CheckReturnValue
	public Object notReturningSelf() {
		return new Object();
	}

	@Override
	public WithCheckReturnValue isEqualTo(Object expected) {
		return super.isEqualTo(expected);
	}

}
