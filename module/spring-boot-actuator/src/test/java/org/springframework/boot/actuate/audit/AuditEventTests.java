/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.audit;

import java.util.Collections;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link AuditEvent}.
 *
 * @author Dave Syer
 * @author Vedran Pavic
 */
class AuditEventTests {

	@Test
	void nowEvent() {
		AuditEvent event = new AuditEvent("phil", "UNKNOWN", Collections.singletonMap("a", "b"));
		assertThat(event.getData()).containsEntry("a", "b");
		assertThat(event.getType()).isEqualTo("UNKNOWN");
		assertThat(event.getPrincipal()).isEqualTo("phil");
		assertThat(event.getTimestamp()).isNotNull();
	}

	@Test
	void convertStringsToData() {
		AuditEvent event = new AuditEvent("phil", "UNKNOWN", "a=b", "c=d");
		assertThat(event.getData()).containsEntry("a", "b");
		assertThat(event.getData()).containsEntry("c", "d");
	}

	@Test
	void nullPrincipalIsMappedToEmptyString() {
		AuditEvent auditEvent = new AuditEvent(null, "UNKNOWN", Collections.singletonMap("a", "b"));
		assertThat(auditEvent.getPrincipal()).isEmpty();
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void nullTimestamp() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new AuditEvent(null, "phil", "UNKNOWN", Collections.singletonMap("a", "b")))
			.withMessageContaining("'timestamp' must not be null");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void nullType() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new AuditEvent("phil", null, Collections.singletonMap("a", "b")))
			.withMessageContaining("'type' must not be null");
	}

	@Test
	void jsonFormat() throws Exception {
		AuditEvent event = new AuditEvent("johannes", "UNKNOWN",
				Collections.singletonMap("type", (Object) "BadCredentials"));
		String json = new JsonMapper().writeValueAsString(event);
		JSONObject jsonObject = new JSONObject(json);
		assertThat(jsonObject.getString("type")).isEqualTo("UNKNOWN");
		assertThat(jsonObject.getJSONObject("data").getString("type")).isEqualTo("BadCredentials");
	}

}
