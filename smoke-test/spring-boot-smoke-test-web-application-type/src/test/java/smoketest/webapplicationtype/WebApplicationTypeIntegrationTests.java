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

package smoketest.webapplicationtype;

import org.junit.jupiter.api.Test;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link WebApplicationType} deducers.
 *
 * @author Phillip Webb
 */
class WebApplicationTypeIntegrationTests {

	@Test
	@ClassPathExclusions("spring-boot-webflux*")
	void deduceWhenNoWebFluxModuleAndWebMvcModule() {
		assertThat(WebApplicationType.deduce()).isEqualTo(WebApplicationType.SERVLET);
	}

	@Test
	@ClassPathExclusions({ "spring-boot-webflux*", "spring-web-*" })
	void deduceWhenNoWebFluxModuleAndNoSpringWebAndWebMvcModule() {
		assertThat(WebApplicationType.deduce()).isEqualTo(WebApplicationType.NONE);
	}

	@Test
	@ClassPathExclusions("spring-boot-webmvc*")
	void deduceWhenNoWebMvcModuleAndWebFluxModule() {
		assertThat(WebApplicationType.deduce()).isEqualTo(WebApplicationType.REACTIVE);
	}

	@Test
	@ClassPathExclusions({ "spring-boot-webmvc*", "spring-webflux-*" })
	void deduceWhenNoWebMvcModuleAndNoWebFluxAndWebFluxModule() {
		assertThat(WebApplicationType.deduce()).isEqualTo(WebApplicationType.SERVLET);
	}

	@Test
	@ClassPathExclusions({ "spring-boot-webmvc*", "spring-webflux-*", "spring-web-*" })
	void deduceWhenNoWebMvcModuleAndNoWebFluxAndNoWebAndWebFluxModule() {
		assertThat(WebApplicationType.deduce()).isEqualTo(WebApplicationType.NONE);
	}

	@Test
	void deduceWhenWebMvcAndWebFlux() {
		assertThat(WebApplicationType.deduce()).isEqualTo(WebApplicationType.SERVLET);
	}

	@Test
	@ClassPathExclusions("spring-webmvc-*")
	void deduceWhenNoWebMvc() {
		assertThat(WebApplicationType.deduce()).isEqualTo(WebApplicationType.REACTIVE);
	}

	@Test
	@ClassPathExclusions({ "spring-webmvc-*", "spring-webflux-*" })
	void deduceWhenNoWebMvcAndNoWebFlux() {
		assertThat(WebApplicationType.deduce()).isEqualTo(WebApplicationType.SERVLET);
	}

	@Test
	@ClassPathExclusions({ "spring-web-*" })
	void deduceWhenNoWeb() {
		assertThat(WebApplicationType.deduce()).isEqualTo(WebApplicationType.NONE);
	}

	@Test
	@ClassPathExclusions(packages = "jakarta.servlet", files = "spring-webflux-*")
	void deduceWhenNoServletOrWebFlux() {
		assertThat(WebApplicationType.deduce()).isEqualTo(WebApplicationType.NONE);
	}

}
