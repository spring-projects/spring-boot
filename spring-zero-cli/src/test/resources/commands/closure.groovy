def run = { msg ->
	org.springframework.bootstrap.cli.command.ScriptCommandTests.executed = true
	println "Hello ${msg}"
}

run