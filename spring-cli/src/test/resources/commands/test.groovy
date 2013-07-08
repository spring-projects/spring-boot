options {
	option "foo", "Foo set"
	option "bar", "Bar has an argument of type int" withOptionalArg() ofType Integer
}

org.springframework.cli.command.ScriptCommandTests.executed = true
println "Hello ${options.nonOptionArguments()}: ${options.has('foo')} ${options.valueOf('bar')}"
