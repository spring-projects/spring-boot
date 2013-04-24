/*
 * Cloud Foundry 2012.02.03 Beta
 * Copyright (c) [2009-2012] VMware, Inc. All Rights Reserved.
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product includes a number of subcomponents with
 * separate copyright notices and license terms. Your use of these
 * subcomponents is subject to the terms and conditions of the
 * subcomponent's license, as noted in the LICENSE file.
 */

package org.springframework.bootstrap.groovy;

import static org.junit.Assert.*

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * @author Dave Syer
 *
 */
class ScriptTests {

	private BashEnv bash
	
	private ExecutorService executor = Executors.newFixedThreadPool(2) 
	
	@Before
	void init() {
		bash = new BashEnv(".", [SPRING_HOME: "target"])
		bash.start()
		bash.execute("export GVM_DIR=~/.gvm")
		bash.execute("source ~/.gvm/bin/gvm-init.sh")
		assertEquals("You need to install gvm to run these tests", 0, bash.status)
		bash.execute("gvm use groovy 2.1.0")
		assertEquals("You need to do this before running the tests: > gvm install groovy 2.1.0", 0, bash.status)
	}

	@After
	void clean() {
		bash?.stop()
	}

	@Test
	void testVanillaApplicationContext() {
		execute(bash, "src/main/scripts/spring src/test/apps/Empty.groovy")
		assertEquals(0, bash.status)
	}

	@Test
	void testBatchApplicationContext() {
		execute(bash, "src/main/scripts/spring src/test/apps/JobConfig.groovy foo=bar")
		assertEquals(0, bash.status)
		assertTrue(bash.output.contains("[SimpleJob: [name=job]] completed with the following parameters: [{foo=bar}]"))
	}

	private void execute(BashEnv bash, String cmdline) {
		bash.execute(cmdline)
		if (bash.exitCode && bash.status!=0) {
			println "Unsuccessful execution (${cmdline}). Output: \n${bash.output}"
		}
	}
}
