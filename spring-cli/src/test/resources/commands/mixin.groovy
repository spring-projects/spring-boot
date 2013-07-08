void options() {
	option "foo", "Foo set"
}

org.springframework.cli.command.ScriptCommandTests.executed = true
println "Hello ${options.nonOptionArguments()}: ${options.has('foo')}"
