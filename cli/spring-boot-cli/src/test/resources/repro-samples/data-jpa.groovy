/*
 * Copyright 2012-present the original author or authors.
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

@Grab('spring-boot-starter-data-jpa')
@Grab('h2')
class Sample implements CommandLineRunner {

	// No Data JPA-based logic. We just want to check that the dependencies are
	// resolved correctly and that the app runs

	@Override
	void run(String... args) {
		println "Hello World"
	}

}
