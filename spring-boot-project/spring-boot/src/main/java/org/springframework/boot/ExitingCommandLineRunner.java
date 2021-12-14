/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * Abstract class that deal with running command line using an exit exception with the exit code
 * uses the class ExitException
 * *
 * @author Raquel Olhovetchi
 * @since 1.0.0
 * @see ApplicationRunner
 */

public abstract class ExitingCommandLineRunner implements CommandLineRunner {

	private final MyCommand myCommand;

	private final IFactory factory; // auto-configured to inject PicocliSpringFactory

	public MyApplicationRunner(MyCommand myCommand, IFactory factory) {
		this.myCommand = myCommand;
		this.factory = factory;
	}

	@Override
	public void run(String... args) throws Exception {
		int exitCode = new CommandLine(myCommand, factory).execute(args);
		if (exitCode != 0) {
			throw new ExitException(exitCode);
		}
	}

}
