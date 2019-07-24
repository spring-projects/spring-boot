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

package sample.data.couchbase;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SampleCouchbaseApplication implements CommandLineRunner {

	@Autowired
	private UserRepository userRepository;

	public static void main(String[] args) {
		SpringApplication.run(SampleCouchbaseApplication.class);
	}

	@Override
	public void run(String... args) throws Exception {
		this.userRepository.deleteAll();
		User user = saveUser();
		System.out.println(this.userRepository.findById(user.getId()));
	}

	private User saveUser() {
		User user = new User();
		user.setId(UUID.randomUUID().toString());
		user.setFirstName("Alice");
		user.setLastName("Smith");
		return this.userRepository.save(user);
	}

}
