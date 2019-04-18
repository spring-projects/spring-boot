/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.test.mock.mockito;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Tests for a mock bean where the class being mocked uses field injection.
 *
 * @author Andy Wilkinson
 */
@RunWith(SpringRunner.class)
public class MockBeanWithInjectedFieldIntegrationTests {

	@MockBean
	private MyService myService;

	@Test
	public void fieldInjectionIntoMyServiceMockIsNotAttempted() {
		given(this.myService.getCount()).willReturn(5);
		assertThat(this.myService.getCount()).isEqualTo(5);
	}

	private static class MyService {

		@Autowired
		private MyRepository repository;

		public int getCount() {
			return this.repository.findAll().size();
		}

	}

	private interface MyRepository {

		List<Object> findAll();

	}

}
