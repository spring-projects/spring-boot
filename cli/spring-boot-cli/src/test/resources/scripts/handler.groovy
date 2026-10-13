/*
 * Copyright 2012-present the original author or authors.
 */

package org.test.command

@Grab("org.eclipse.jgit:org.eclipse.jgit:2.3.1.201302201838-r")
import org.eclipse.jgit.api.Git


class TestCommand extends OptionHandler {
	
	String name = "foo"

	void options() {
		option "foo", "Foo set"
	}

	void run(OptionSet options) {
		// Demonstrate use of Grape.grab to load dependencies before running
		println "Clean: " + Git.open(".." as File).status().call().isClean()
		println "Hello ${options.nonOptionArguments()}: ${options.has('foo')}"
	}

}
