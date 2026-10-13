/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.opentelemetry;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.resources.Resource;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SampleOpenTelemetryApplicationTests {

	@Autowired
	private Resource resource;

	@Test
	void resourceAttributesAreMapped() {
		assertThat(this.resource.getAttribute(AttributeKey.stringKey("service.name"))).isEqualTo("my-service");
		assertThat(this.resource.getAttribute(AttributeKey.stringKey("service.version"))).isEqualTo("1.0.0");
	}

}
