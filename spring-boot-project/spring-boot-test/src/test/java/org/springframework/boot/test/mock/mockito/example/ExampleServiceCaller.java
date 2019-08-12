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

package org.springframework.boot.test.mock.mockito.example;

/**
 * Example bean for mocking tests that calls {@link ExampleService}.
 *
 * @author Phillip Webb
 */
public class ExampleServiceCaller {

	private final ExampleService service;

	public ExampleServiceCaller(ExampleService service) {
		this.service = service;
	}

	public ExampleService getService() {
		return this.service;
	}

	public String sayGreeting() {
		return "I say " + this.service.greeting();
	}

}
