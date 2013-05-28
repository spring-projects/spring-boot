package org.test.command

class TestCommand implements Command {

	String name = "foo"
	
	String description = "My script command"
	
	String help = "No options"

	String usageHelp = "Not very useful"

	void run(String... args) {
		org.springframework.bootstrap.cli.command.ScriptCommandTests.executed = true
		println "Hello ${args[0]}"
	}

}
