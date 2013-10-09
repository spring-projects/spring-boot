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

import joptsimple.OptionSet

@Grab("org.eclipse.jgit:org.eclipse.jgit:2.3.1.201302201838-r")
import org.eclipse.jgit.api.Git


class TestCommand extends OptionHandler {

	void options() {
		option "foo", "Foo set"
	}

	void run(OptionSet options) {
		// Demonstrate use of Grape.grab to load dependencies before running
		println "Clean : " + Git.open(".." as File).status().call().isClean()
		org.springframework.boot.cli.command.ScriptCommandTests.executed = true
		println "Hello ${options.nonOptionArguments()}: ${options.has('foo')}"
	}

}
