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

package org.springframework.boot;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;

/**
 Class used to help to make command line runner with an exit code without a mutable state, it
 dealing with exit exception and receiving the exit code
 * @author Raquel Olhovetchi
 * @since 1.0.0
 * @see ApplicationRunner
 */
public class ExitException implements ExitCodeGenerator {

	private final int code;

	ExitException(int code) {
		this.code = code;
	}

	@Override
	public int getExitCode() {
		return this.code;
	}

}