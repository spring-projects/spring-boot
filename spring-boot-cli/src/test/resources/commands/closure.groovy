def run = { msg ->
	org.springframework.boot.cli.command.ScriptCommandTests.executed = true
	println "Hello ${msg}"
}

run
