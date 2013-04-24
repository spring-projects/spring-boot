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
import org.junit.Ignore
import org.junit.Test
import org.springframework.web.client.RestTemplate

/**
 * @author Dave Syer
 *
 */
class WebScriptTests {

	private BashEnv bash
	
	private ExecutorService executor = Executors.newFixedThreadPool(2) 
	
	private File messages = new File("target/messages")

	@Before
	void init() {
		assertTrue("Couldn't delete messages directory", messages.deleteDir())
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
		killit()
		bash?.stop()
	}

	@Test
	@Ignore
	void testWebApplicationContext() {
		executor.submit {
			execute(bash, "src/main/scripts/spring src/test/apps/App.groovy src/test/apps/Signal.groovy")
		}
		File ready = new File(messages, "ready")
		long timeout = 10000
		long t0 = System.currentTimeMillis()
		while (!ready.exists() && System.currentTimeMillis() - t0 < timeout) {
			println "Waiting for app to start"
			Thread.sleep(1000)
		}
		if (ready.exists()) {
			println ready.text
		} else {
			fail("Timed out waiting for app to start")
		}

		// assertEquals(0, bash.status)
		def response = new RestTemplate().getForEntity("http://localhost:8080", String.class)
		assertEquals("Hello World!", response.body)
	}
	
	private void killit() {
		BashEnv reaper = new BashEnv(".", [SPRING_HOME: "target"])
		reaper.start()
		reaper?.execute("pkill -9 -f '\\--configscript src/main/scripts/customizer.groovy'")
		reaper?.stop()
	}
	
	private void execute(BashEnv bash, String cmdline) {
		bash.execute(cmdline)
		if (bash.exitCode && bash.status!=0) {
			println "Unsuccessful execution (${cmdline}). Output: \n${bash.output}"
		}
	}
}
