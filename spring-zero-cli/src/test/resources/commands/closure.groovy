def run = { msg ->
	org.springframework.zero.cli.command.ScriptCommandTests.executed = true
	println "Hello ${msg}"
}

run
