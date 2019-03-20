/*
 * Copyright 2012-2014 the original author or authors.
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

package sample.data.solr;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SampleSolrApplication implements CommandLineRunner {

	@Autowired
	private ProductRepository repository;

	@Override
	public void run(String... args) throws Exception {

		this.repository.deleteAll();

		// insert some products
		this.repository.save(new Product("1", "Nintendo Entertainment System"));
		this.repository.save(new Product("2", "Sega Megadrive"));
		this.repository.save(new Product("3", "Sony Playstation"));

		// fetch all
		System.out.println("Products found by findAll():");
		System.out.println("----------------------------");
		for (Product product : this.repository.findAll()) {
			System.out.println(product);
		}
		System.out.println();

		// fetch a single product
		System.out.println("Products found with findByNameStartingWith('So'):");
		System.out.println("--------------------------------");
		for (Product product : this.repository.findByNameStartingWith("So")) {
			System.out.println(product);
		}
		System.out.println();
	}

	public static void main(String[] args) throws Exception {
		SpringApplication.run(SampleSolrApplication.class, args);
	}

}
