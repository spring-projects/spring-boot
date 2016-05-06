package org.springframework.boot.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class TestRestTemplateTests {

	@Test
	public void canCreateTemplateFromOwnOptions() {
		TestRestTemplate template = new TestRestTemplate(TestRestTemplate.HttpClientOption.ENABLE_REDIRECTS);
		assertThat(template).isNotNull();
	}

}
