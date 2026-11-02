/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.logging.logback;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.boot.json.JsonWriter.MemberPath;
import org.springframework.boot.json.JsonWriter.Members;
import org.springframework.boot.json.JsonWriter.NameProcessor;
import org.springframework.boot.logging.structured.StructuredLogFormatter;
import org.springframework.boot.logging.structured.StructuredLoggingJsonMembersCustomizer;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link StructuredLogEncoder}.
 *
 * @author Moritz Halbritter
 * @author Phillip Webb
 * @author Diego Magalhães
 */
class StructuredLogEncoderTests extends AbstractStructuredLoggingTests {

	private StructuredLogEncoder encoder;

	private Context loggerContext;

	private MockEnvironment environment;

	@Override
	@BeforeEach
	void setUp() {
		super.setUp();
		this.environment = new MockEnvironment();
		this.environment.setProperty("logging.structured.json.stacktrace.printer",
				SimpleStackTracePrinter.class.getName());
		this.loggerContext = new ContextBase();
		this.loggerContext.putObject(Environment.class.getName(), this.environment);
		this.encoder = new StructuredLogEncoder();
		this.encoder.setContext(this.loggerContext);
	}

	@Override
	@AfterEach
	void tearDown() {
		super.tearDown();
		this.encoder.stop();
	}

	@Test
	@SuppressWarnings("unchecked")
	void shouldSupportEcsCommonFormat() {
		this.encoder.setFormat("ecs");
		this.encoder.start();
		LoggingEvent event = createEvent(new RuntimeException("Boom!"));
		event.setMDCPropertyMap(Collections.emptyMap());
		String json = encode(event);
		Map<String, Object> deserialized = deserialize(json);
		assertThat(deserialized).containsEntry("ecs", Map.of("version", "8.11"));
		Map<String, Object> error = (Map<String, Object>) deserialized.get("error");
		assertThat(error).isNotNull();
		assertThat(error.get("stack_trace")).isEqualTo("stacktrace:RuntimeException");
	}

	@Test
	@SuppressWarnings("unchecked")
	void shouldOutputNestedAdditionalEcsJson() {
		this.environment.setProperty("logging.structured.json.add.extra.value", "test");
		this.encoder.setFormat("ecs");
		this.encoder.start();
		LoggingEvent event = createEvent();
		event.setMDCPropertyMap(Collections.emptyMap());
		String json = encode(event);
		Map<String, Object> deserialized = deserialize(json);
		assertThat(deserialized).containsKey("extra");
		assertThat((Map<String, Object>) deserialized.get("extra")).containsEntry("value", "test");
		System.out.println(deserialized);
	}

	@Test
	void shouldSupportLogstashCommonFormat() {
		this.encoder.setFormat("logstash");
		this.encoder.start();
		LoggingEvent event = createEvent(new RuntimeException("Boom!"));
		event.setMDCPropertyMap(Collections.emptyMap());
		String json = encode(event);
		Map<String, Object> deserialized = deserialize(json);
		assertThat(deserialized).containsKey("@version");
		assertThat(deserialized.get("stack_trace")).isEqualTo("stacktrace:RuntimeException");
	}

	@Test
	void shouldSupportGelfCommonFormat() {
		this.encoder.setFormat("gelf");
		this.encoder.start();
		LoggingEvent event = createEvent(new RuntimeException("Boom!"));
		event.setMDCPropertyMap(Collections.emptyMap());
		String json = encode(event);
		Map<String, Object> deserialized = deserialize(json);
		assertThat(deserialized).containsKey("version");
		assertThat(deserialized.get("_error_stack_trace")).isEqualTo("stacktrace:RuntimeException");
	}

	@Test
	void shouldSupportCustomFormat() {
		this.encoder.setFormat(CustomLogbackStructuredLoggingFormatter.class.getName());
		this.encoder.start();
		LoggingEvent event = createEvent();
		event.setMDCPropertyMap(Collections.emptyMap());
		String format = encode(event);
		assertThat(format).isEqualTo("custom-format");
	}

	@Test
	void shouldInjectCustomFormatConstructorParameters() {
		this.environment.setProperty("spring.application.pid", "42");
		this.encoder.setFormat(CustomLogbackStructuredLoggingFormatterWithInjection.class.getName());
		this.encoder.start();
		LoggingEvent event = createEvent();
		event.setMDCPropertyMap(Collections.emptyMap());
		String format = encode(event);
		assertThat(format).isEqualTo("custom-format-with-injection pid=42 hasThrowableProxyConverter=true");
	}

	@Test
	void shouldCheckTypeArgument() {
		assertThatIllegalStateException().isThrownBy(() -> {
			this.encoder.setFormat(CustomLogbackStructuredLoggingFormatterWrongType.class.getName());
			this.encoder.start();
		}).withMessageContaining("must be ch.qos.logback.classic.spi.ILoggingEvent but was java.lang.String");
	}

	@Test
	void shouldCheckTypeArgumentWithRawType() {
		assertThatIllegalStateException().isThrownBy(() -> {
			this.encoder.setFormat(CustomLogbackStructuredLoggingFormatterRawType.class.getName());
			this.encoder.start();
		}).withMessageContaining("must be ch.qos.logback.classic.spi.ILoggingEvent but was null");
	}

	@Test
	void shouldFailIfNoCommonOrCustomFormatIsSet() {
		assertThatIllegalArgumentException().isThrownBy(() -> {
			this.encoder.setFormat("does-not-exist");
			this.encoder.start();
		})
			.withMessageContaining("Unknown format 'does-not-exist'. Values can be a valid fully-qualified "
					+ "class name or one of the common formats: [ecs, gelf, logstash]");
	}

	private String encode(LoggingEvent event) {
		return new String(this.encoder.encode(event), StandardCharsets.UTF_8);
	}

	static final class CustomLogbackStructuredLoggingFormatter implements StructuredLogFormatter<ILoggingEvent> {

		@Override
		public String format(ILoggingEvent event) {
			return "custom-format";
		}

	}

	static final class CustomLogbackStructuredLoggingFormatterWithInjection
			implements StructuredLogFormatter<ILoggingEvent> {

		private final Environment environment;

		private final ThrowableProxyConverter throwableProxyConverter;

		CustomLogbackStructuredLoggingFormatterWithInjection(Environment environment,
				ThrowableProxyConverter throwableProxyConverter) {
			this.environment = environment;
			this.throwableProxyConverter = throwableProxyConverter;
		}

		@Override
		public String format(ILoggingEvent event) {
			boolean hasThrowableProxyConverter = this.throwableProxyConverter != null;
			return "custom-format-with-injection pid=" + this.environment.getProperty("spring.application.pid")
					+ " hasThrowableProxyConverter=" + hasThrowableProxyConverter;
		}

	}

	static final class CustomLogbackStructuredLoggingFormatterWrongType implements StructuredLogFormatter<String> {

		@Override
		public String format(String event) {
			return event;
		}

	}

	@SuppressWarnings("rawtypes")
	static final class CustomLogbackStructuredLoggingFormatterRawType implements StructuredLogFormatter {

		@Override
		public String format(Object event) {
			return "";
		}

	}

	/**
	 * Tests for logging.structured.json properties.
	 */
	@Nested
	class JsonPropertiesTests {

		private static final String TRACE_ID = "4bf92f3577b34da6a3ce929d0e0e4736";

		private static final String SPAN_ID = "00f067aa0ba902b7";

		private static final String PROPERTIES_LAMBDA_RETURNED_EMPTY = "^NameProcessor org\\.springframework\\.boot\\."
				+ "logging\\.structured\\.StructuredLoggingJsonPropertiesJsonMembersCustomizer\\$\\$Lambda\\S* "
				+ "returned an empty result$";

		private static final String ECS = """
				{"@timestamp":"2024-07-02T08:49:53Z","log":{"level":"INFO","logger":"org.example.Test"},"process":{"thread":{"name":"main"}},"message":"message","traceId":"4bf92f3577b34da6a3ce929d0e0e4736","spanId":"00f067aa0ba902b7","ecs":{"version":"8.11"}}
				""";

		private static final String LOGSTASH = """
				{"@timestamp":"%s","@version":"1","message":"message","logger_name":"org.example.Test","thread_name":"main","level":"INFO","level_value":20000,"traceId":"4bf92f3577b34da6a3ce929d0e0e4736","spanId":"00f067aa0ba902b7"}
				""";

		private static final String GELF = """
				{"version":"1.1","short_message":"message","timestamp":1719910193.000,"level":6,"_level_name":"INFO","_process_thread_name":"main","_log_logger":"org.example.Test","_traceId":"4bf92f3577b34da6a3ce929d0e0e4736","_spanId":"00f067aa0ba902b7"}
				""";

		private static final String ECS_RENAMED = """
				{"@timestamp":"2024-07-02T08:49:53Z","log":{"severity":"INFO","logger":"org.example.Test"},"process":{"thread":{"thread_name":"main"}},"message":"message","trace_id":"4bf92f3577b34da6a3ce929d0e0e4736","spanId":"00f067aa0ba902b7","ecs":{"v":"8.11"}}
				""";

		private static final String LOGSTASH_RENAMED = """
				{"@timestamp":"%s","@version":"1","message":"message","logger":"org.example.Test","thread_name":"main","level":"INFO","level_value":20000,"trace_id":"4bf92f3577b34da6a3ce929d0e0e4736","spanId":"00f067aa0ba902b7"}
				""";

		private static final String GELF_RENAMED = """
				{"version":"1.1","short_message":"message","timestamp":1719910193.000,"level":6,"_level":"INFO","_process_thread_name":"main","_log_logger":"org.example.Test","_trace":"4bf92f3577b34da6a3ce929d0e0e4736","_spanId":"00f067aa0ba902b7"}
				""";

		@Test
		void whenNoJsonPropertyIsSet() {
			LoggingEvent event = s5Event();
			assertThat(encode("ecs", Collections.emptyMap(), event)).isEqualTo(ECS);
			assertThat(encode("logstash", Collections.emptyMap(), event)).isEqualTo(logstash(LOGSTASH));
			assertThat(encode("gelf", Collections.emptyMap(), event)).isEqualTo(GELF);
		}

		@Test
		void whenOnlyAddIsSet() {
			Map<String, String> properties = Map.of("logging.structured.json.add.environment", "perf");
			LoggingEvent event = s5Event();
			String ecs = """
					{"@timestamp":"2024-07-02T08:49:53Z","log":{"level":"INFO","logger":"org.example.Test"},"process":{"thread":{"name":"main"}},"message":"message","traceId":"4bf92f3577b34da6a3ce929d0e0e4736","spanId":"00f067aa0ba902b7","ecs":{"version":"8.11"},"environment":"perf"}
					""";
			String logstash = """
					{"@timestamp":"%s","@version":"1","message":"message","logger_name":"org.example.Test","thread_name":"main","level":"INFO","level_value":20000,"traceId":"4bf92f3577b34da6a3ce929d0e0e4736","spanId":"00f067aa0ba902b7","environment":"perf"}
					""";
			String gelf = """
					{"version":"1.1","short_message":"message","timestamp":1719910193.000,"level":6,"_level_name":"INFO","_process_thread_name":"main","_log_logger":"org.example.Test","_traceId":"4bf92f3577b34da6a3ce929d0e0e4736","_spanId":"00f067aa0ba902b7","environment":"perf"}
					""";
			assertThat(encode("ecs", properties, event)).isEqualTo(ecs);
			assertThat(encode("logstash", properties, event)).isEqualTo(logstash(logstash));
			assertThat(encode("gelf", properties, event)).isEqualTo(gelf);
		}

		@Test
		void whenOnlyStackTracePrinterIsSet() {
			Map<String, String> properties = Map.of("logging.structured.json.stacktrace.printer",
					SimpleStackTracePrinter.class.getName());
			LoggingEvent event = event(new RuntimeException("Boom!"), "traceId", TRACE_ID, "spanId", SPAN_ID);
			String ecs = """
					{"@timestamp":"2024-07-02T08:49:53Z","log":{"level":"INFO","logger":"org.example.Test"},"process":{"thread":{"name":"main"}},"message":"message","traceId":"4bf92f3577b34da6a3ce929d0e0e4736","spanId":"00f067aa0ba902b7","error":{"type":"java.lang.RuntimeException","message":"Boom!","stack_trace":"stacktrace:RuntimeException"},"ecs":{"version":"8.11"}}
					""";
			String logstash = """
					{"@timestamp":"%s","@version":"1","message":"message","logger_name":"org.example.Test","thread_name":"main","level":"INFO","level_value":20000,"traceId":"4bf92f3577b34da6a3ce929d0e0e4736","spanId":"00f067aa0ba902b7","stack_trace":"stacktrace:RuntimeException"}
					""";
			String gelf = """
					{"version":"1.1","short_message":"message","timestamp":1719910193.000,"level":6,"_level_name":"INFO","_process_thread_name":"main","_log_logger":"org.example.Test","_traceId":"4bf92f3577b34da6a3ce929d0e0e4736","_spanId":"00f067aa0ba902b7","full_message":"message\\n\\nstacktrace:RuntimeException","_error_type":"java.lang.RuntimeException","_error_stack_trace":"stacktrace:RuntimeException","_error_message":"Boom!"}
					""";
			assertThat(encode("ecs", properties, event)).isEqualTo(ecs);
			assertThat(encode("logstash", properties, event)).isEqualTo(logstash(logstash));
			assertThat(encode("gelf", properties, event)).isEqualTo(gelf);
		}

		@Test
		void whenRenameIsSet() {
			LoggingEvent event = s5Event();
			assertThat(encode("ecs", renameProperties("ecs"), event)).isEqualTo(ECS_RENAMED);
			assertThat(encode("logstash", renameProperties("logstash"), event)).isEqualTo(logstash(LOGSTASH_RENAMED));
			assertThat(encode("gelf", renameProperties("gelf"), event)).isEqualTo(GELF_RENAMED);
		}

		@Test
		void whenRenameMatchesNoPath() {
			Map<String, String> properties = Map.of("logging.structured.json.rename[nope]", "x");
			LoggingEvent event = s5Event();
			assertThat(encode("ecs", properties, event)).isEqualTo(ECS);
			assertThat(encode("logstash", properties, event)).isEqualTo(logstash(LOGSTASH));
			assertThat(encode("gelf", properties, event)).isEqualTo(GELF);
		}

		@Test
		void whenRenamePathContainsDots() {
			Map<String, String> properties = Map.of("logging.structured.json.rename[a.b]", "x");
			LoggingEvent event = event("a.b", "v");
			String ecs = """
					{"@timestamp":"2024-07-02T08:49:53Z","log":{"level":"INFO","logger":"org.example.Test"},"process":{"thread":{"name":"main"}},"message":"message","a":{"x":"v"},"ecs":{"version":"8.11"}}
					""";
			String logstash = """
					{"@timestamp":"%s","@version":"1","message":"message","logger_name":"org.example.Test","thread_name":"main","level":"INFO","level_value":20000,"x":"v"}
					""";
			String gelf = """
					{"version":"1.1","short_message":"message","timestamp":1719910193.000,"level":6,"_level_name":"INFO","_process_thread_name":"main","_log_logger":"org.example.Test","_a.b":"v"}
					""";
			assertThat(encode("ecs", properties, event)).isEqualTo(ecs);
			assertThat(encode("logstash", properties, event)).isEqualTo(logstash(logstash));
			assertThat(encode("gelf", properties, event)).isEqualTo(gelf);
		}

		@Test
		void whenRenameIncludeAndExcludeAreSet() {
			LoggingEvent event = s5Event();
			Map<String, String> ecs = Map.of("logging.structured.json.include", "log,log.level,message,traceId",
					"logging.structured.json.exclude", "traceId", "logging.structured.json.rename[message]", "msg");
			Map<String, String> logstash = Map.of("logging.structured.json.include", "level,message,traceId",
					"logging.structured.json.exclude", "traceId", "logging.structured.json.rename[message]", "msg");
			Map<String, String> gelf = Map.of("logging.structured.json.include", "level,short_message,_traceId",
					"logging.structured.json.exclude", "_traceId", "logging.structured.json.rename[short_message]",
					"msg");
			assertThat(encode("ecs", ecs, event)).isEqualTo("{\"log\":{\"level\":\"INFO\"},\"msg\":\"message\"}\n");
			assertThat(encode("logstash", logstash, event)).isEqualTo("{\"msg\":\"message\",\"level\":\"INFO\"}\n");
			assertThat(encode("gelf", gelf, event)).isEqualTo("{\"msg\":\"message\",\"level\":6}\n");
		}

		@Test
		void whenNameProcessorReturnsEmptyThrowsException() {
			Map<String, String> customizer = Map.of("logging.structured.json.customizer",
					EmptyingCustomizer.class.getName());
			LoggingEvent event = s5Event();
			assertThatIllegalStateException().isThrownBy(() -> encode("ecs", customizer, event))
				.withMessage("NameProcessor EmptyingNameProcessor returned an empty result");
			assertThatIllegalStateException().isThrownBy(() -> encode("logstash", customizer, event))
				.withMessage("NameProcessor EmptyingNameProcessor returned an empty result");
			assertThat(encode("gelf", customizer, event)).isEqualTo(GELF);
			Map<String, String> prefix = Map.of("logging.structured.json.context.prefix", "ctx.");
			assertThatIllegalStateException().isThrownBy(() -> encode("ecs", prefix, event(".z", "3")))
				.withMessageMatching(PROPERTIES_LAMBDA_RETURNED_EMPTY);
		}

		@Test
		void whenOnlyContextPrefixIsSet() {
			Map<String, String> properties = Map.of("logging.structured.json.context.prefix", "ctx");
			LoggingEvent event = event("traceId", TRACE_ID, "_x", "1", ".z", "3");
			String ecs = """
					{"@timestamp":"2024-07-02T08:49:53Z","log":{"level":"INFO","logger":"org.example.Test"},"process":{"thread":{"name":"main"}},"message":"message","ctx":{"traceId":"4bf92f3577b34da6a3ce929d0e0e4736","_x":"1","z":"3"},"ecs":{"version":"8.11"}}
					""";
			String logstash = """
					{"@timestamp":"%s","@version":"1","message":"message","logger_name":"org.example.Test","thread_name":"main","level":"INFO","level_value":20000,"ctx_traceId":"4bf92f3577b34da6a3ce929d0e0e4736","ctx_x":"1","ctx_.z":"3"}
					""";
			String gelf = """
					{"version":"1.1","short_message":"message","timestamp":1719910193.000,"level":6,"_level_name":"INFO","_process_thread_name":"main","_log_logger":"org.example.Test","_ctxtraceId":"4bf92f3577b34da6a3ce929d0e0e4736","_ctx_x":"1","_ctx.z":"3"}
					""";
			assertThat(encode("ecs", properties, event)).isEqualTo(ecs);
			assertThat(encode("logstash", properties, event)).isEqualTo(logstash(logstash));
			assertThat(encode("gelf", properties, event)).isEqualTo(gelf);
		}

		@Test
		void whenRenameValueIsEmpty() {
			Map<String, String> properties = Map.of("logging.structured.json.rename[message]", "");
			LoggingEvent event = s5Event();
			assertThatIllegalStateException().isThrownBy(() -> encode("ecs", properties, event))
				.withMessageMatching(PROPERTIES_LAMBDA_RETURNED_EMPTY);
			assertThatIllegalStateException().isThrownBy(() -> encode("logstash", properties, event))
				.withMessageMatching(PROPERTIES_LAMBDA_RETURNED_EMPTY);
			assertThat(encode("gelf", properties, event)).isEqualTo(GELF);
		}

		@Test
		void encodeInEachCharset() {
			Map<String, String> add = Map.of("logging.structured.json.add.environment", "perf");
			LoggingEvent event = event("traceId", TRACE_ID, "spanId", SPAN_ID, "user", "Jos\u00e9 \u20ac \uD83D\uDE00");
			String ecsWithAdd = """
					{"@timestamp":"2024-07-02T08:49:53Z","log":{"level":"INFO","logger":"org.example.Test"},"process":{"thread":{"name":"main"}},"message":"message","traceId":"4bf92f3577b34da6a3ce929d0e0e4736","spanId":"00f067aa0ba902b7","user":"Jos\u00e9 \u20ac \uD83D\uDE00","ecs":{"version":"8.11"},"environment":"perf"}
					""";
			String logstashWithAdd = """
					{"@timestamp":"%s","@version":"1","message":"message","logger_name":"org.example.Test","thread_name":"main","level":"INFO","level_value":20000,"traceId":"4bf92f3577b34da6a3ce929d0e0e4736","spanId":"00f067aa0ba902b7","user":"Jos\u00e9 \u20ac \uD83D\uDE00","environment":"perf"}
					""";
			String gelfWithAdd = """
					{"version":"1.1","short_message":"message","timestamp":1719910193.000,"level":6,"_level_name":"INFO","_process_thread_name":"main","_log_logger":"org.example.Test","_traceId":"4bf92f3577b34da6a3ce929d0e0e4736","_spanId":"00f067aa0ba902b7","_user":"Jos\u00e9 \u20ac \uD83D\uDE00","environment":"perf"}
					""";
			String ecsWithRename = """
					{"@timestamp":"2024-07-02T08:49:53Z","log":{"severity":"INFO","logger":"org.example.Test"},"process":{"thread":{"thread_name":"main"}},"message":"message","trace_id":"4bf92f3577b34da6a3ce929d0e0e4736","spanId":"00f067aa0ba902b7","user":"Jos\u00e9 \u20ac \uD83D\uDE00","ecs":{"v":"8.11"}}
					""";
			String logstashWithRename = """
					{"@timestamp":"%s","@version":"1","message":"message","logger":"org.example.Test","thread_name":"main","level":"INFO","level_value":20000,"trace_id":"4bf92f3577b34da6a3ce929d0e0e4736","spanId":"00f067aa0ba902b7","user":"Jos\u00e9 \u20ac \uD83D\uDE00"}
					""";
			String gelfWithRename = """
					{"version":"1.1","short_message":"message","timestamp":1719910193.000,"level":6,"_level":"INFO","_process_thread_name":"main","_log_logger":"org.example.Test","_trace":"4bf92f3577b34da6a3ce929d0e0e4736","_spanId":"00f067aa0ba902b7","_user":"Jos\u00e9 \u20ac \uD83D\uDE00"}
					""";
			Map<String, String> expectedWithAdd = Map.of("ecs", ecsWithAdd, "logstash", logstash(logstashWithAdd),
					"gelf", gelfWithAdd);
			Map<String, String> expectedWithRename = Map.of("ecs", ecsWithRename, "logstash",
					logstash(logstashWithRename), "gelf", gelfWithRename);
			for (Charset charset : List.of(StandardCharsets.UTF_8, StandardCharsets.ISO_8859_1,
					StandardCharsets.US_ASCII, StandardCharsets.UTF_16)) {
				expectedWithAdd.forEach((format, expected) -> assertThat(encode(format, add, event, charset))
					.as("%s add %s", format, charset)
					.isEqualTo(bytes(expected, charset)));
				expectedWithRename
					.forEach((format, expected) -> assertThat(encode(format, renameProperties(format), event, charset))
						.as("%s rename %s", format, charset)
						.isEqualTo(bytes(expected, charset)));
			}
		}

		@Test
		void encodeRepeatedlyOnSameThread() {
			LoggingEvent duplicate = event("a", "1", "a.b", "2");
			LoggingEvent user = event("user", "alice");
			String ecsUser = """
					{"@timestamp":"2024-07-02T08:49:53Z","log":{"severity":"INFO","logger":"org.example.Test"},"process":{"thread":{"thread_name":"main"}},"message":"message","user":"alice","ecs":{"v":"8.11"}}
					""";
			String logstashDuplicate = """
					{"@timestamp":"%s","@version":"1","message":"message","logger":"org.example.Test","thread_name":"main","level":"INFO","level_value":20000,"a":"1","a.b":"2"}
					""";
			String logstashUser = """
					{"@timestamp":"%s","@version":"1","message":"message","logger":"org.example.Test","thread_name":"main","level":"INFO","level_value":20000,"user":"alice"}
					""";
			String gelfDuplicate = """
					{"version":"1.1","short_message":"message","timestamp":1719910193.000,"level":6,"_level":"INFO","_process_thread_name":"main","_log_logger":"org.example.Test","_a":"1","_a.b":"2"}
					""";
			String gelfUser = """
					{"version":"1.1","short_message":"message","timestamp":1719910193.000,"level":6,"_level":"INFO","_process_thread_name":"main","_log_logger":"org.example.Test","_user":"alice"}
					""";
			StructuredLogEncoder ecs = createEncoder("ecs", renameProperties("ecs"), null);
			try {
				assertThat(encode(ecs, s5Event())).isEqualTo(ECS_RENAMED)
					.isEqualTo(encode("ecs", renameProperties("ecs"), s5Event()));
				assertThatIllegalStateException().isThrownBy(() -> ecs.encode(duplicate))
					.withMessage("Duplicate nested pairs added under 'a'");
				assertThat(encode(ecs, user)).isEqualTo(ecsUser);
			}
			finally {
				ecs.stop();
			}
			StructuredLogEncoder logstash = createEncoder("logstash", renameProperties("logstash"), null);
			try {
				assertThat(encode(logstash, s5Event())).isEqualTo(logstash(LOGSTASH_RENAMED))
					.isEqualTo(encode("logstash", renameProperties("logstash"), s5Event()));
				assertThat(encode(logstash, duplicate)).isEqualTo(logstash(logstashDuplicate));
				assertThat(encode(logstash, user)).isEqualTo(logstash(logstashUser));
			}
			finally {
				logstash.stop();
			}
			StructuredLogEncoder gelf = createEncoder("gelf", renameProperties("gelf"), null);
			try {
				assertThat(encode(gelf, s5Event())).isEqualTo(GELF_RENAMED)
					.isEqualTo(encode("gelf", renameProperties("gelf"), s5Event()));
				assertThat(encode(gelf, duplicate)).isEqualTo(gelfDuplicate);
				assertThat(encode(gelf, user)).isEqualTo(gelfUser);
			}
			finally {
				gelf.stop();
			}
		}

		private Map<String, String> renameProperties(String format) {
			Map<String, String> properties = new LinkedHashMap<>();
			switch (format) {
				case "ecs" -> {
					properties.put("logging.structured.json.rename[process.thread.name]", "thread_name");
					properties.put("logging.structured.json.rename[log.level]", "severity");
					properties.put("logging.structured.json.rename[traceId]", "trace_id");
					properties.put("logging.structured.json.rename[ecs.version]", "v");
				}
				case "logstash" -> {
					properties.put("logging.structured.json.rename[logger_name]", "logger");
					properties.put("logging.structured.json.rename[traceId]", "trace_id");
				}
				default -> {
					properties.put("logging.structured.json.rename[_level_name]", "_level");
					properties.put("logging.structured.json.rename[_traceId]", "_trace");
				}
			}
			return properties;
		}

		private LoggingEvent s5Event() {
			return event("traceId", TRACE_ID, "spanId", SPAN_ID);
		}

		private LoggingEvent event(String... mdc) {
			return event(null, mdc);
		}

		private LoggingEvent event(@Nullable Throwable thrown, String... mdc) {
			LoggingEvent event = createEvent("message", thrown);
			Map<String, String> map = new LinkedHashMap<>();
			for (int i = 0; i < mdc.length; i += 2) {
				map.put(mdc[i], mdc[i + 1]);
			}
			event.setMDCPropertyMap(map);
			return event;
		}

		private String logstash(String json) {
			return json.formatted(DateTimeFormatter.ISO_OFFSET_DATE_TIME
				.format(OffsetDateTime.ofInstant(EVENT_TIME, ZoneId.systemDefault())));
		}

		private String encode(String format, Map<String, String> properties, ILoggingEvent event) {
			return new String(encode(format, properties, event, StandardCharsets.UTF_8), StandardCharsets.UTF_8);
		}

		private byte[] encode(String format, Map<String, String> properties, ILoggingEvent event, Charset charset) {
			StructuredLogEncoder encoder = createEncoder(format, properties, charset);
			try {
				return encoder.encode(event);
			}
			finally {
				encoder.stop();
			}
		}

		private String encode(StructuredLogEncoder encoder, ILoggingEvent event) {
			return new String(encoder.encode(event), StandardCharsets.UTF_8);
		}

		private StructuredLogEncoder createEncoder(String format, Map<String, String> properties,
				@Nullable Charset charset) {
			MockEnvironment environment = new MockEnvironment();
			properties.forEach(environment::setProperty);
			ContextBase context = new ContextBase();
			context.putObject(Environment.class.getName(), environment);
			StructuredLogEncoder encoder = new StructuredLogEncoder();
			encoder.setContext(context);
			encoder.setCharset(charset);
			encoder.setFormat(format);
			encoder.start();
			return encoder;
		}

		private byte[] bytes(String json, Charset charset) {
			try {
				ByteBuffer buffer = charset.newEncoder()
					.onMalformedInput(CodingErrorAction.REPLACE)
					.onUnmappableCharacter(CodingErrorAction.REPLACE)
					.encode(CharBuffer.wrap(json));
				byte[] bytes = new byte[buffer.remaining()];
				buffer.get(bytes);
				return bytes;
			}
			catch (CharacterCodingException ex) {
				throw new IllegalStateException(ex);
			}
		}

		/**
		 * Customizer that adds an {@link EmptyingNameProcessor}.
		 */
		static class EmptyingCustomizer implements StructuredLoggingJsonMembersCustomizer<Object> {

			@Override
			public void customize(Members<Object> members) {
				members.applyingNameProcessor(new EmptyingNameProcessor());
			}

		}

		/**
		 * {@link NameProcessor} that returns an empty name for {@code message}.
		 */
		static class EmptyingNameProcessor implements NameProcessor {

			@Override
			public String processName(MemberPath path, String existingName) {
				return "message".equals(existingName) ? "" : existingName;
			}

			@Override
			public String toString() {
				return "EmptyingNameProcessor";
			}

		}

	}

}
