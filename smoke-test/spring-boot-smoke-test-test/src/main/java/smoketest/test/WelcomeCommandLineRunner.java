/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.test;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Simple component that just prints a message. Used to show how different types of
 * integration tests work.
 *
 * @author Phillip Webb
 */
@Component
public class WelcomeCommandLineRunner implements CommandLineRunner {

	@Override
	public void run(String... args) throws Exception {
		System.out.println("***** WELCOME TO THE DEMO *****");
	}

}
