/*
 * Copyright 2012-2017 the original author or authors.
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
 * Example bean for mocking tests that calls {@link ExampleGenericService}.
 *
 * @author Phillip Webb
 */
public class ExampleGenericServiceCaller {

	private final ExampleGenericService<Integer> integerService;

	private final ExampleGenericService<String> stringService;

	public ExampleGenericServiceCaller(ExampleGenericService<Integer> integerService,
			ExampleGenericService<String> stringService) {
		this.integerService = integerService;
		this.stringService = stringService;
	}

	public ExampleGenericService<Integer> getIntegerService() {
		return this.integerService;
	}

	public ExampleGenericService<String> getStringService() {
		return this.stringService;
	}

	public String sayGreeting() {
		return "I say " + this.integerService.greeting() + " "
				+ this.stringService.greeting();
	}

}
