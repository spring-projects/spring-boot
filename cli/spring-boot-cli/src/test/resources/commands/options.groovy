/*
 * Copyright 2012-present the original author or authors.
 */

 def foo() {
	 "Foo"
 }

command("foo") {

	options {
		option "foo", "A foo of type String"
		option "bar", "Bar has a value" withOptionalArg() ofType Integer
	}

	run { options ->
		println "Hello ${foo()}: bar=${options.valueOf('bar')}"
	}

}
