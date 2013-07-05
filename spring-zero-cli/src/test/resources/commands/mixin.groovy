void options() {
	option "foo", "Foo set"
}

org.springframework.bootstrap.cli.command.ScriptCommandTests.executed = true
println "Hello ${options.nonOptionArguments()}: ${options.has('foo')}"
