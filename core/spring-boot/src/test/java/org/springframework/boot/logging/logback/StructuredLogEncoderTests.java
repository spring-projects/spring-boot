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
import java.util.ArrayList;
import java.util.Arrays;
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
import org.slf4j.event.KeyValuePair;

import org.springframework.boot.logging.structured.ContextPairs;
import org.springframework.boot.logging.structured.JsonWriterStructuredLogFormatter;
import org.springframework.boot.logging.structured.StructuredLogFormatter;
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

	/**
	 * Tests for context data written by the common formats.
	 */
	@Nested
	class ContextDataTests {

		private static final String TRACE_ID = "4bf92f3577b34da6a3ce929d0e0e4736";

		private static final String SPAN_ID = "00f067aa0ba902b7";

		private static final String S5_MEMBERS = "\"traceId\":\"" + TRACE_ID + "\",\"spanId\":\"" + SPAN_ID
				+ "\",\"orderId\":4711";

		private static final String PREFIX = "logging.structured.json.context.prefix";

		@Test
		void ecsWhenNamesHaveNoDots() {
			startEncoder("ecs");
			assertThat(encode(s5Event())).isEqualTo(ecs(S5_MEMBERS));
		}

		@Test
		void ecsWhenNamesHaveDots() {
			startEncoder("ecs");
			assertThat(encode(dottedNamesEvent(true)))
				.isEqualTo(ecs("\"\":{\"y\":\"1\"},\"p\":{\"\":{\"q\":\"2\"}},\"x\":\"3\",\".\":\"6\","
						+ "\"a\":{\"b\":{\"c\":\"8\",\"d\":\"9\"}}"));
		}

		@Test
		void ecsWhenNameIsDuplicatedThrowsException() {
			startEncoder("ecs");
			assertThatIllegalStateException().isThrownBy(() -> encode(event(mdc("a", "1", "a.b", "2"))))
				.withMessage("Duplicate nested pairs added under 'a'");
			assertThatIllegalStateException().isThrownBy(() -> encode(event(mdc("a.b", "1", "a", "2"))))
				.withMessage("Duplicate nested pairs added under 'a'");
			assertThatIllegalStateException().isThrownBy(() -> encode(sameNameInMdcAndKeyValuesEvent()))
				.withMessage("Duplicate nested pairs added under 'traceId'");
		}

		@Test
		void ecsWhenNullValueIsFollowedBySameName() {
			startEncoder("ecs");
			assertThat(encode(event(mdc("k", null), new KeyValuePair("k", "v")))).isEqualTo(ecs("\"k\":\"v\""));
			assertThat(encode(event(mdc("k", null)))).isEqualTo(ecs("\"k\":null"));
		}

		@Test
		void logstashWhenNamesAreFlat() {
			startEncoder("logstash");
			assertThat(encode(s5Event())).isEqualTo(logstash(S5_MEMBERS));
			assertThat(encode(dottedNamesEvent(true))).isEqualTo(logstash(
					"\".y\":\"1\",\"p..q\":\"2\",\"x.\":\"3\",\".\":\"5\",\"..\":\"6\",\"a.b.c\":\"8\",\"a.b.d\":\"9\""));
		}

		@Test
		void logstashWhenNameIsDuplicatedThrowsException() {
			startEncoder("logstash");
			assertThatIllegalStateException().isThrownBy(() -> encode(sameNameInMdcAndKeyValuesEvent()))
				.withMessage("The name 'traceId' has already been written");
			assertThatIllegalStateException()
				.isThrownBy(() -> encode(event(mdc("k", null), new KeyValuePair("k", "v"))))
				.withMessage("The name 'k' has already been written");
		}

		@Test
		void gelfWithContextData() {
			startEncoder("gelf");
			assertThat(encode(s5Event()))
				.isEqualTo(gelf("\"_traceId\":\"" + TRACE_ID + "\",\"_spanId\":\"" + SPAN_ID + "\",\"_orderId\":4711"));
			assertThat(encode(dottedNamesEvent(false)))
				.isEqualTo(gelf("\"_.y\":\"1\",\"_p..q\":\"2\",\"_x.\":\"3\",\"_.\":\"5\",\"_..\":\"6\",\"_\":\"7\","
						+ "\"_a.b.c\":\"8\",\"_a.b.d\":\"9\""));
			Map<@Nullable String, @Nullable String> nullName = mdc();
			nullName.put(null, "10");
			assertThat(encode(event(nullName))).isEqualTo(gelf("\"_\":\"10\""));
		}

		@Test
		void whenContextPrefixIsSet() {
			String traceId = "\":\"" + TRACE_ID + "\"";
			assertThat(encodePrefixEvent("ecs", "ctx"))
				.isEqualTo(ecs("\"ctx\":{\"traceId" + traceId + ",\"_x\":\"1\",\"z\":\"3\"}"));
			assertThatIllegalStateException().isThrownBy(() -> encodePrefixEvent("ecs", "ctx."))
				.withMessageMatching("^NameProcessor org\\.springframework\\.boot\\.logging\\.structured\\."
						+ "StructuredLoggingJsonPropertiesJsonMembersCustomizer\\$\\$Lambda\\S* returned an empty result$");
			assertThat(encodePrefixEvent("ecs", "ctx_"))
				.isEqualTo(ecs("\"ctx_\":{\"traceId" + traceId + ",\"_x\":\"1\",\"z\":\"3\"}"));
			assertThat(encodePrefixEvent("logstash", "ctx"))
				.isEqualTo(logstash("\"ctx_traceId" + traceId + ",\"ctx_x\":\"1\",\"ctx_.z\":\"3\""));
			assertThat(encodePrefixEvent("logstash", "ctx."))
				.isEqualTo(logstash("\"ctx._traceId" + traceId + ",\"ctx._x\":\"1\",\"ctx._.z\":\"3\""));
			assertThat(encodePrefixEvent("logstash", "ctx_"))
				.isEqualTo(logstash("\"ctx_traceId" + traceId + ",\"ctx__x\":\"1\",\"ctx_.z\":\"3\""));
			assertThat(encodePrefixEvent("gelf", "ctx"))
				.isEqualTo(gelf("\"_ctxtraceId" + traceId + ",\"_ctx_x\":\"1\",\"_ctx.z\":\"3\""));
			assertThat(encodePrefixEvent("gelf", "ctx."))
				.isEqualTo(gelf("\"_ctx.traceId" + traceId + ",\"_ctx._x\":\"1\",\"_ctx..z\":\"3\""));
			assertThat(encodePrefixEvent("gelf", "ctx_"))
				.isEqualTo(gelf("\"_ctx_traceId" + traceId + ",\"_ctx__x\":\"1\",\"_ctx_.z\":\"3\""));
		}

		@Test
		void whenContextIncludeIsFalse() {
			String include = "logging.structured.json.context.include";
			startEncoder("ecs", include, "false");
			assertThat(encode(s5Event())).isEqualTo(ecs(""));
			startEncoder("logstash", include, "false");
			assertThat(encode(s5Event())).isEqualTo(logstash(""));
			startEncoder("gelf", include, "false");
			assertThat(encode(s5Event())).isEqualTo(gelf(""));
		}

		@Test
		void ecsWhenAddPropertyIsSet() {
			startEncoder("ecs", "logging.structured.json.add.corp", "mycorp", "logging.structured.json.add.extra.value",
					"test");
			assertThat(encode(s5Event()))
				.isEqualTo(ecs(S5_MEMBERS, "\"corp\":\"mycorp\",\"extra\":{\"value\":\"test\"}"));
		}

		@Test
		void whenNamesAreNonAsciiOrLong() {
			for (NamesCase namesCase : nonAsciiOrLongNames()) {
				startEncoder("ecs");
				assertThat(encode(event(namesCase.mdc()))).as(namesCase.description())
					.isEqualTo(readBackAsUtf8(ecs(namesCase.ecs())));
				startEncoder("logstash");
				assertThat(encode(event(namesCase.mdc()))).as(namesCase.description())
					.isEqualTo(readBackAsUtf8(logstash(namesCase.logstash())));
			}
		}

		@Test
		void customFormatUsingContextPairs() {
			String traceId = "\":\"" + TRACE_ID + "\"";
			assertThat(encodeCustomFormat(FlatDotFormatter.class, null))
				.isEqualTo("{\"traceId" + traceId + ",\".a\":\"1\",\"_b\":\"2\",\"c.d\":\"3\"}\n");
			assertThat(encodeCustomFormat(FlatUnderscoreFormatter.class, null))
				.isEqualTo("{\"traceId" + traceId + ",\".a\":\"1\",\"_b\":\"2\",\"c.d\":\"3\"}\n");
			assertThat(encodeCustomFormat(NestedFormatter.class, null))
				.isEqualTo("{\"traceId" + traceId + ",\"\":{\"a\":\"1\"},\"_b\":\"2\",\"c\":{\"d\":\"3\"}}\n");
			assertThat(encodeCustomFormat(FlatDotFormatter.class, "ctx"))
				.isEqualTo("{\"ctx.traceId" + traceId + ",\"ctx.a\":\"1\",\"ctx._b\":\"2\",\"ctx.c.d\":\"3\"}\n");
			assertThat(encodeCustomFormat(FlatUnderscoreFormatter.class, "ctx"))
				.isEqualTo("{\"ctx_traceId" + traceId + ",\"ctx_.a\":\"1\",\"ctx_b\":\"2\",\"ctx_c.d\":\"3\"}\n");
			assertThat(encodeCustomFormat(NestedFormatter.class, "ctx"))
				.isEqualTo("{\"ctx\":{\"traceId" + traceId + ",\"a\":\"1\",\"_b\":\"2\",\"c\":{\"d\":\"3\"}}}\n");
		}

		@Test
		void encodeInEachCharset() {
			List<Charset> charsets = List.of(StandardCharsets.UTF_8, StandardCharsets.ISO_8859_1,
					StandardCharsets.US_ASCII, StandardCharsets.UTF_16);
			for (Charset charset : charsets) {
				assertEncodedBytes("ecs", charset, "S5", s5Event(), ecs(S5_MEMBERS));
				assertEncodedBytes("logstash", charset, "S5", s5Event(), logstash(S5_MEMBERS));
				for (NamesCase namesCase : nonAsciiOrLongNames()) {
					assertEncodedBytes("ecs", charset, namesCase.description(), event(namesCase.mdc()),
							ecs(namesCase.ecs()));
					assertEncodedBytes("logstash", charset, namesCase.description(), event(namesCase.mdc()),
							logstash(namesCase.logstash()));
				}
			}
		}

		private void assertEncodedBytes(String format, Charset charset, String description, LoggingEvent event,
				String expectedJson) {
			startEncoder(format, charset);
			assertThat(StructuredLogEncoderTests.this.encoder.encode(event))
				.as("%s %s %s", format, charset, description)
				.isEqualTo(encode(expectedJson, charset));
		}

		@Test
		void encodeRepeatedlyOnSameThread() {
			startEncoder("ecs");
			String first = encode(s5Event());
			assertThat(first).isEqualTo(ecs(S5_MEMBERS));
			assertThatIllegalStateException().isThrownBy(() -> encode(sameNameInMdcAndKeyValuesEvent()));
			assertThat(encode(event(mdc("user", "alice")))).isEqualTo(ecs("\"user\":\"alice\""));
			startEncoder("ecs");
			assertThat(encode(s5Event())).isEqualTo(first);
		}

		private List<NamesCase> nonAsciiOrLongNames() {
			String longName = "k".repeat(10_000);
			String dottedName = String.join(".", Collections.nCopies(200, "s"));
			String nested = "\"s\":{".repeat(199) + "\"s\":\"4\"" + "}".repeat(199);
			return List.of(new NamesCase("non-ASCII and long",
					mdc("caf\u00e9", "1", "\u65e5\u672c.\u8a9e", "2", longName, "3", dottedName, "4"),
					"\"caf\u00e9\":\"1\",\"\u65e5\u672c\":{\"\u8a9e\":\"2\"},\"" + longName + "\":\"3\"," + nested,
					"\"caf\u00e9\":\"1\",\"\u65e5\u672c.\u8a9e\":\"2\",\"" + longName + "\":\"3\",\"" + dottedName
							+ "\":\"4\""),
					new NamesCase("surrogates without dots", mdc("\uD83D\uDE00", "1", "\uD83D", "2", "x\uDE00", "3"),
							"\"\uD83D\uDE00\":\"1\",\"\uD83D\":\"2\",\"x\uDE00\":\"3\"",
							"\"\uD83D\uDE00\":\"1\",\"\uD83D\":\"2\",\"x\uDE00\":\"3\""),
					new NamesCase("surrogates with dots",
							mdc("a.\uD83D\uDE00", "1", "\uD83D.b", "2", "c.\uDE00.d", "3"),
							"\"a\":{\"\uD83D\uDE00\":\"1\"},\"\uD83D\":{\"b\":\"2\"},\"c\":{\"\uDE00\":{\"d\":\"3\"}}",
							"\"a.\uD83D\uDE00\":\"1\",\"\uD83D.b\":\"2\",\"c.\uDE00.d\":\"3\""),
					new NamesCase("controls without dots",
							mdc("\u0001", "v\u0002", "k\u0003", "\u0004", "\u0000x", "\u001F"),
							"\"\\u0001\":\"v\\u0002\",\"k\\u0003\":\"\\u0004\",\"\\u0000x\":\"\\u001F\"",
							"\"\\u0001\":\"v\\u0002\",\"k\\u0003\":\"\\u0004\",\"\\u0000x\":\"\\u001F\""),
					new NamesCase("controls with dots", mdc("a.\u0001", "1", "\u0002.b", "2", "c\u007F.d", "\u0003"),
							"\"a\":{\"\\u0001\":\"1\"},\"\\u0002\":{\"b\":\"2\"},\"c\\u007F\":{\"d\":\"\\u0003\"}",
							"\"a.\\u0001\":\"1\",\"\\u0002.b\":\"2\",\"c\\u007F.d\":\"\\u0003\""));
		}

		private void startEncoder(String format, String... properties) {
			startEncoder(format, StandardCharsets.UTF_8, properties);
		}

		private void startEncoder(String format, Charset charset, String... properties) {
			StructuredLogEncoderTests.this.encoder.stop();
			MockEnvironment environment = new MockEnvironment();
			for (int i = 0; i < properties.length; i += 2) {
				environment.setProperty(properties[i], properties[i + 1]);
			}
			Context context = new ContextBase();
			context.putObject(Environment.class.getName(), environment);
			StructuredLogEncoder encoder = new StructuredLogEncoder();
			encoder.setContext(context);
			encoder.setCharset(charset);
			encoder.setFormat(format);
			encoder.start();
			StructuredLogEncoderTests.this.encoder = encoder;
		}

		private String encodePrefixEvent(String format, String prefix) {
			startEncoder(format, PREFIX, prefix);
			return encode(event(mdc("traceId", TRACE_ID, "_x", "1", ".z", "3")));
		}

		private String encodeCustomFormat(Class<?> formatter, @Nullable String prefix) {
			if (prefix != null) {
				startEncoder(formatter.getName(), PREFIX, prefix);
			}
			else {
				startEncoder(formatter.getName());
			}
			return encode(event(mdc("traceId", TRACE_ID, ".a", "1", "_b", "2", "c.d", "3")));
		}

		private LoggingEvent s5Event() {
			return event(mdc("traceId", TRACE_ID, "spanId", SPAN_ID), new KeyValuePair("orderId", 4711));
		}

		private LoggingEvent dottedNamesEvent(boolean withNullName) {
			Map<@Nullable String, @Nullable String> mdc = mdc(".y", "1", "p..q", "2", "x.", "3", ".", "5", "..", "6",
					"", "7", "a.b.c", "8", "a.b.d", "9");
			if (withNullName) {
				mdc.put(null, "10");
			}
			return event(mdc);
		}

		private LoggingEvent sameNameInMdcAndKeyValuesEvent() {
			return event(mdc("traceId", TRACE_ID), new KeyValuePair("traceId", "kv"));
		}

		private LoggingEvent event(Map<@Nullable String, @Nullable String> mdc, KeyValuePair... keyValuePairs) {
			LoggingEvent event = createEvent();
			event.setMDCPropertyMap(mdc);
			if (keyValuePairs.length > 0) {
				event.setKeyValuePairs(new ArrayList<>(Arrays.asList(keyValuePairs)));
			}
			return event;
		}

		private Map<@Nullable String, @Nullable String> mdc(@Nullable String... namesAndValues) {
			Map<@Nullable String, @Nullable String> mdc = new LinkedHashMap<>();
			for (int i = 0; i < namesAndValues.length; i += 2) {
				mdc.put(namesAndValues[i], namesAndValues[i + 1]);
			}
			return mdc;
		}

		private String ecs(String context) {
			return ecs(context, "");
		}

		private String ecs(String context, String added) {
			return "{\"@timestamp\":\"2024-07-02T08:49:53Z\",\"log\":{\"level\":\"INFO\",\"logger\":\"org.example.Test\"},"
					+ "\"process\":{\"thread\":{\"name\":\"main\"}},\"message\":\"message\"" + members(context)
					+ ",\"ecs\":{\"version\":\"8.11\"}" + members(added) + "}\n";
		}

		private String logstash(String context) {
			String timestamp = DateTimeFormatter.ISO_OFFSET_DATE_TIME
				.format(OffsetDateTime.ofInstant(EVENT_TIME, ZoneId.systemDefault()));
			return "{\"@timestamp\":\"" + timestamp + "\",\"@version\":\"1\",\"message\":\"message\","
					+ "\"logger_name\":\"org.example.Test\",\"thread_name\":\"main\",\"level\":\"INFO\","
					+ "\"level_value\":20000" + members(context) + "}\n";
		}

		private String gelf(String context) {
			return "{\"version\":\"1.1\",\"short_message\":\"message\",\"timestamp\":1719910193.000,\"level\":6,"
					+ "\"_level_name\":\"INFO\",\"_process_thread_name\":\"main\",\"_log_logger\":\"org.example.Test\""
					+ members(context) + "}\n";
		}

		private String members(String members) {
			return (!members.isEmpty()) ? "," + members : "";
		}

		private String readBackAsUtf8(String json) {
			return new String(encode(json, StandardCharsets.UTF_8), StandardCharsets.UTF_8);
		}

		private byte[] encode(String json, Charset charset) {
			try {
				ByteBuffer bytes = charset.newEncoder()
					.onMalformedInput(CodingErrorAction.REPLACE)
					.onUnmappableCharacter(CodingErrorAction.REPLACE)
					.encode(CharBuffer.wrap(json));
				byte[] result = new byte[bytes.remaining()];
				bytes.get(result);
				return result;
			}
			catch (CharacterCodingException ex) {
				throw new IllegalStateException(ex);
			}
		}

		private String encode(LoggingEvent event) {
			return StructuredLogEncoderTests.this.encode(event);
		}

		record NamesCase(String description, Map<@Nullable String, @Nullable String> mdc, String ecs, String logstash) {

		}

		static final class FlatDotFormatter extends JsonWriterStructuredLogFormatter<ILoggingEvent> {

			FlatDotFormatter(ContextPairs contextPairs) {
				super((members) -> members.add()
					.usingPairs(
							contextPairs.flat(".", (pairs) -> pairs.addMapEntries(ILoggingEvent::getMDCPropertyMap))),
						null);
			}

		}

		static final class FlatUnderscoreFormatter extends JsonWriterStructuredLogFormatter<ILoggingEvent> {

			FlatUnderscoreFormatter(ContextPairs contextPairs) {
				super((members) -> members.add()
					.usingPairs(
							contextPairs.flat("_", (pairs) -> pairs.addMapEntries(ILoggingEvent::getMDCPropertyMap))),
						null);
			}

		}

		static final class NestedFormatter extends JsonWriterStructuredLogFormatter<ILoggingEvent> {

			NestedFormatter(ContextPairs contextPairs) {
				super((members) -> members.add()
					.usingPairs(contextPairs.nested((pairs) -> pairs.addMapEntries(ILoggingEvent::getMDCPropertyMap))),
						null);
			}

		}

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

}
