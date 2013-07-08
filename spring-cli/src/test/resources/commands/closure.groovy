def run = { msg ->
	org.springframework.cli.command.ScriptCommandTests.executed = true
	println "Hello ${msg}"
}

run
