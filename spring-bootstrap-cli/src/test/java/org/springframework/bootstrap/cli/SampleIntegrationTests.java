/*
 * Copyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.bootstrap.cli;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Dave Syer
 * 
 */
public class SampleIntegrationTests {

	private RunCommand command;

	private void start(final String sample) throws Exception {
		Future<RunCommand> future = Executors.newSingleThreadExecutor().submit(
				new Callable<RunCommand>() {
					@Override
					public RunCommand call() throws Exception {
						RunCommand command = new RunCommand();
						command.run(sample);
						return command;
					}
				});
		this.command = future.get(10, TimeUnit.SECONDS);
	}

	@After
	public void stop() {
		if (this.command != null) {
			this.command.stop();
		}
	}

	@BeforeClass
	public static void clean() {
		// SpringBootstrapCli.main("clean");
		// System.setProperty("ivy.message.logger.level", "3");
	}

	@Test
	public void appSample() throws Exception {
		start("samples/app.groovy");
	}

	@Test
	public void jobSample() throws Exception {
		start("samples/job.groovy");
	}

	@Test
	public void webSample() throws Exception {
		start("samples/web.groovy");
	}

	@Test
	public void serviceSample() throws Exception {
		start("samples/service.groovy");
	}

	@Test
	public void integrationSample() throws Exception {
		start("samples/integration.groovy");
	}

}
