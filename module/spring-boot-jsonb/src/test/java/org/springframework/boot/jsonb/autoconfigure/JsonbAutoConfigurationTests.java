/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jsonb.autoconfigure;

import jakarta.json.bind.Jsonb;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JsonbAutoConfiguration}.
 *
 * @author Eddú Meléndez
 */
class JsonbAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(JsonbAutoConfiguration.class));

	@Test
	void jsonbRegistration() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(Jsonb.class);
			Jsonb jsonb = context.getBean(Jsonb.class);
			assertThat(jsonb.toJson(new DataObject())).isEqualTo("{\"data\":\"hello\"}");
		});
	}

	public class DataObject {

		private String data = "hello";

		public String getData() {
			return this.data;
		}

		public void setData(String data) {
			this.data = data;
		}

	}

}
