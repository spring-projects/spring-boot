/*
 * Copyright 2012-2024 the original author or authors.
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

package smoketest.data.jpa;

import org.junit.jupiter.api.Test;
import smoketest.data.jpa.service.CityRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

/**
 * Tests for {@link SampleDataJpaApplication} that use {@link SpyBean @SpyBean}.
 *
 * @author Andy Wilkinson
 * @deprecated since 3.4.0 for removal in 3.6.0
 */
@SuppressWarnings("removal")
@Deprecated(since = "3.4.0", forRemoval = true)
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
class SpyBeanSampleDataJpaApplicationTests {

	@Autowired
	private MockMvcTester mvc;

	@SpyBean
	private CityRepository repository;

	@Test
	void testHome() {
		assertThat(this.mvc.get().uri("/")).hasStatusOk().hasBodyTextEqualTo("Bath");
		then(this.repository).should().findByNameAndCountryAllIgnoringCase("Bath", "UK");
	}

}
