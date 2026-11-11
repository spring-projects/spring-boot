/*
 * Copyright 2012-present the original author or authors.
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
