package org.springframework.boot.logging.log4j2;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

/**
 * Tests for {@link WhitespaceThrowablePatternConverter}.
 *
 * @author Vladimir Tsanev
 */
public class WhitespaceThrowablePatternConverterTests {

	private static final String LINE_SEPARATOR = System.getProperty("line.separator");

	private final WhitespaceThrowablePatternConverter converter = WhitespaceThrowablePatternConverter
			.newInstance(new String[] {});

	@Test
	public void noStackTrace() throws Exception {
		LogEvent event = Log4jLogEvent.newBuilder().build();
		StringBuilder builder = new StringBuilder();
		this.converter.format(event, builder);
		assertThat(builder.toString(), equalTo(""));
	}

	@Test
	public void withStackTrace() throws Exception {
		LogEvent event = Log4jLogEvent.newBuilder().setThrown(new Exception()).build();
		StringBuilder builder = new StringBuilder();
		this.converter.format(event, builder);
		assertThat(builder.toString(), startsWith(LINE_SEPARATOR));
		assertThat(builder.toString(), endsWith(LINE_SEPARATOR));
	}

}
