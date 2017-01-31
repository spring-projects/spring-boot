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

package org.springframework.boot.autoconfigure.thymeleaf;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfigIntegrationTestBase.TestConfig;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for {@link ThymeleafAutoConfiguration} with alternate index specified
 *
 * @author Bruce Brouwer
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes=TestConfig.class, properties="spring.thymeleaf.root-view=alternateIndex")
public class RootViewAlternateIntegrationTests extends ThymeleafAutoConfigIntegrationTestBase {

    @Test
    public void index() throws Exception {
		MvcResult response = this.mockMvc
				.perform(get("/").accept(MediaType.TEXT_HTML))
				.andExpect(status().isOk()).andReturn();
		String content = response.getResponse().getContentAsString();
		assertThat(content).isEqualTo("<html lang=\"en\"><body>Alternate Index</body></html>");
    }
}
