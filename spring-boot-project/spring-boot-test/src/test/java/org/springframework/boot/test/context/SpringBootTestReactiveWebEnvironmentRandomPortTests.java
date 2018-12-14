/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.test.context;

import org.junit.runner.RunWith;

import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.config.EnableWebFlux;

/**
 * Tests for {@link SpringBootTest} in a reactive environment configured with
 * {@link WebEnvironment#RANDOM_PORT}.
 *
 * @author Stephane Nicoll
 */
@DirtiesContext
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = {
		"spring.main.webApplicationType=reactive", "value=123" })
@RunWith(SpringRunner.class)
public class SpringBootTestReactiveWebEnvironmentRandomPortTests
		extends AbstractSpringBootTestEmbeddedReactiveWebEnvironmentTests {

	@Configuration
	@EnableWebFlux
	@RestController
	protected static class Config extends AbstractConfig {

	}

}
