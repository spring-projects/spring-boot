/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.jarmode.tools;

import java.io.PrintStream;
import java.util.List;
import java.util.Map;

/**
 * @author Moritz Halbritter
 */
class TestCommand extends Command {

	TestCommand() {
		super("test", "Description of test",
				Options.of(Option.of("option1", "value1", "Description of option1"),
						Option.of("option2", "value2", "Description of option2")),
				Parameters.of("parameter1", "parameter2"));
	}

	@Override
	protected void run(PrintStream out, Map<Option, String> options, List<String> parameters) {

	}

}
