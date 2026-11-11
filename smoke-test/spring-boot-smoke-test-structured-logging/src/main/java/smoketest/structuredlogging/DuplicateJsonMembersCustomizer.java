/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.structuredlogging;

import java.util.Objects;

import org.springframework.boot.json.JsonWriter.Members;
import org.springframework.boot.logging.structured.StructuredLoggingJsonMembersCustomizer;

public class DuplicateJsonMembersCustomizer implements StructuredLoggingJsonMembersCustomizer<Object> {

	@Override
	public void customize(Members<Object> members) {
		members.add("test").as(Objects::toString);
		members.add("test").as(Objects::toString);
	}

}
