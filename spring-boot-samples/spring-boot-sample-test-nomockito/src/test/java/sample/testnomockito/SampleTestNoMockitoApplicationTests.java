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

package sample.testnomockito;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that {@code ResetMocksTestExecutionListener} and
 * {@code MockitoTestExecutionListener} gracefully degrade when Mockito is not on the
 * classpath.
 *
 * @author Madhura Bhave
 */
@RunWith(SpringRunner.class)
public class SampleTestNoMockitoApplicationTests {

	// gh-7065

	@Autowired
	private ApplicationContext context;

	@Test
	public void contextLoads() throws Exception {
		assertThat(this.context).isNotNull();
	}

}
