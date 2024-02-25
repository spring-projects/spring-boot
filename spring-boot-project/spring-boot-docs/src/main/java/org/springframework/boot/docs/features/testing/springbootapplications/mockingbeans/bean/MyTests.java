/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.docs.features.testing.springbootapplications.mockingbeans.bean;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * MyTests class.
 */
@SpringBootTest
class MyTests {

	@Autowired
	private Reverser reverser;

	@MockBean
	private RemoteService remoteService;

	/**
	 * This is an example test method. It tests the functionality of the reverseValue
	 * method in the MyTests class. It uses the given method from the remoteService to set
	 * the value to "spring". Then it calls the getReverseValue method to get the reverse
	 * of the value. Finally, it asserts that the reverse value is equal to "gnirps".
	 */
	@Test
	void exampleTest() {
		given(this.remoteService.getValue()).willReturn("spring");
		String reverse = this.reverser.getReverseValue(); // Calls injected RemoteService
		assertThat(reverse).isEqualTo("gnirps");
	}

}
