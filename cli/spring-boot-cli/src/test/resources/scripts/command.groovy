/*
 * Copyright 2012-present the original author or authors.
 */

package org.test.command

import java.util.Collection

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
