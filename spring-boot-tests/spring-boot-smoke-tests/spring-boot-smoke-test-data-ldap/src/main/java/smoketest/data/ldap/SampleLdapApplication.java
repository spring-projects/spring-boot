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

package smoketest.data.ldap;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SampleLdapApplication implements CommandLineRunner {

	private final PersonRepository repository;

	public SampleLdapApplication(PersonRepository repository) {
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
		SpringApplication.run(SampleLdapApplication.class, args).close();
	}

}
