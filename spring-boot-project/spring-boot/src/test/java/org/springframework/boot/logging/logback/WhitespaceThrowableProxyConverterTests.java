/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.logging.logback;

import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WhitespaceThrowableProxyConverter}.
 *
 * @author Phillip Webb
 * @author Chanwit Kaewkasi
 */
public class WhitespaceThrowableProxyConverterTests {

	private final WhitespaceThrowableProxyConverter converter = new WhitespaceThrowableProxyConverter();

	private final LoggingEvent event = new LoggingEvent();

	@Test
	public void noStackTrace() {
		String s = this.converter.convert(this.event);
		assertThat(s).isEqualTo("");
	}

	@Test
	public void withStackTrace() {
		this.event.setThrowableProxy(new ThrowableProxy(new RuntimeException()));
		String s = this.converter.convert(this.event);
		assertThat(s).startsWith(System.lineSeparator()).endsWith(System.lineSeparator());
	}

}
