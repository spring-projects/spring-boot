package org.springframework.boot.actuate.info;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JavaInfoContributorTests {
	@Test
	void javaInfoShouldBeAdded() {
		JavaInfoContributor javaInfoContributor = new JavaInfoContributor();
		Info.Builder builder = new Info.Builder();
		javaInfoContributor.contribute(builder);

		@SuppressWarnings("unchecked")
		Map<String, Object> javaDetails = (Map<String, Object>) builder.build().getDetails().get("java");
		assertThat(javaDetails.get("vendor")).isEqualTo(System.getProperty("java.vendor"));

		@SuppressWarnings("unchecked")
		Map<String, Object> jreDetails = (Map<String, Object>) javaDetails.get("runtime");
		assertThat(jreDetails.get("name")).isEqualTo(System.getProperty("java.runtime.name"));
		assertThat(jreDetails.get("version")).isEqualTo(System.getProperty("java.runtime.version"));

		@SuppressWarnings("unchecked")
		Map<String, Object> jvmDetails = (Map<String, Object>) javaDetails.get("vm");
		assertThat(jvmDetails.get("name")).isEqualTo(System.getProperty("java.vm.name"));
		assertThat(jvmDetails.get("version")).isEqualTo(System.getProperty("java.vm.version"));
	}
}
