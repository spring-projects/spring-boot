/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.data.ldap;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SampleDataLdapApplication implements CommandLineRunner {

	private final PersonRepository repository;

	public SampleDataLdapApplication(PersonRepository repository) {
		this.repository = repository;
	}

	@Override
	public void run(String... args) throws Exception {

		// fetch all people
		System.out.println("People found with findAll():");
		System.out.println("-------------------------------");
		for (Person person : this.repository.findAll()) {
			System.out.println(person);
		}
		System.out.println();

		// fetch an individual person
		System.out.println("Person found with findByPhone('+46 555-123456'):");
		System.out.println("--------------------------------");
		System.out.println(this.repository.findByPhone("+46 555-123456"));
	}

	public static void main(String[] args) {
		SpringApplication.run(SampleDataLdapApplication.class, args).close();
	}

}
