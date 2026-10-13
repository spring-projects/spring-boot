/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.contributor;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link Status}.
 *
 * @author Phillip Webb
 */
class StatusTests {

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void createWhenCodeIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new Status(null, ""))
			.withMessage("'code' must not be null");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void createWhenDescriptionIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new Status("code", null))
			.withMessage("'description' must not be null");
	}

	@Test
	void getCodeReturnsCode() {
		Status status = new Status("spring", "boot");
		assertThat(status.getCode()).isEqualTo("spring");
	}

	@Test
	void getDescriptionReturnsDescription() {
		Status status = new Status("spring", "boot");
		assertThat(status.getDescription()).isEqualTo("boot");
	}

	@Test
	void equalsAndHashCode() {
		Status one = new Status("spring", "boot");
		Status two = new Status("spring", "framework");
		Status three = new Status("spock", "framework");
		assertThat(one).isEqualTo(one).isEqualTo(two).isNotEqualTo(three);
		assertThat(one).hasSameHashCodeAs(two);
	}

	@Test
	void toStringReturnsCode() {
		assertThat(Status.OUT_OF_SERVICE.getCode()).isEqualTo("OUT_OF_SERVICE");
	}

	@Test
	void serializeWithJacksonReturnsValidJson() throws Exception {
		Status status = new Status("spring", "boot");
		JsonMapper mapper = new JsonMapper();
		String json = mapper.writeValueAsString(status);
		assertThat(json).isEqualTo("{\"description\":\"boot\",\"status\":\"spring\"}");
	}

}
