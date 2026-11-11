/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.structuredlogging;

import java.util.function.UnaryOperator;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.json.JsonWriter.Members;
import org.springframework.boot.json.JsonWriter.ValueProcessor;
import org.springframework.boot.logging.structured.StructuredLoggingJsonMembersCustomizer;

public class SampleJsonMembersCustomizer implements StructuredLoggingJsonMembersCustomizer<Object> {

	@Override
	public void customize(Members<Object> members) {
		UnaryOperator<@Nullable String> formatted = "!!%s!!"::formatted;
		members.applyingValueProcessor(
				ValueProcessor.of(String.class, formatted).whenHasUnescapedPath("process.thread.name"));
	}

}
