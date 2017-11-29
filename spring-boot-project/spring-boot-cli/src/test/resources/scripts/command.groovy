/*
 * Copyright 2012-2013 the original author or authors.
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

package org.test.command

import java.util.Collection;

class TestCommand implements Command {

	String name = "foo"

	String description = "My script command"

	String help = "No options"

	String usageHelp = "Not very useful"
	
	Collection<String> optionsHelp = ["No options"]
	
	boolean optionCommand = false

	void run(String... args) {
		println "Hello ${args[0]}"
	}

}
