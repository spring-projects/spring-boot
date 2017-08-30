/*
 * Copyright 2012-2017 the original author or authors.
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

package sample.data.mongoreactive;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@SpringBootApplication
public class SampleMongoReactiveApplication implements CommandLineRunner {

	@Autowired
	private ReactiveCustomerRepository repository;

	public void run(String... args) throws Exception {
		System.out.println("Deleting all Customers");
		this.repository.deleteAll().then().subscribe();

		// save a couple of customers
		this.repository.saveAll(
				Flux.just(new Customer("Alice", "Smith"), new Customer("Bob", "Smith")))
				.then().subscribe();

		// fetch all customers
		System.out.println("Customers found with findAll():");
		System.out.println("-------------------------------");

		repository.findAll().subscribe(System.out::println);

		System.out.println();

		// fetch an individual customer
		System.out.println("Customer found with findByFirstName('Alice'):");
		System.out.println("--------------------------------");
		this.repository.findByFirstName(Mono.just("Alice")).log()
				.subscribe(System.out::println);

		System.out.println("Customers found with findByLastName('Smith'):");
		System.out.println("--------------------------------");
		this.repository.findByLastName(Mono.just("Smith")).log()
				.subscribe(System.out::println);
	}

	public static void main(String[] args) throws Exception {
		SpringApplication.run(SampleMongoReactiveApplication.class, args);
	}
}
