/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.autoconfigureclassic;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SampleAutoConfigureClassicApplication implements CommandLineRunner {

	@Override
	public void run(String... args) throws Exception {
		System.out.println("Auto-Configure Classic!");
	}

	public static void main(String[] args) {
		SpringApplication.run(SampleAutoConfigureClassicApplication.class, args);
	}

}
