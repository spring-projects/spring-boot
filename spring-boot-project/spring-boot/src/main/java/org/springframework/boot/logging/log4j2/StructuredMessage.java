/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.logging.log4j2;

import java.io.IOException;

import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.MultiFormatStringBuilderFormattable;

import org.springframework.boot.json.WritableJson;

/**
 * Helper used to adapt {@link Message} for structured writing.
 *
 * @author Phillip Webb
 */
final class StructuredMessage {

	private static final String JSON2 = "JSON";

	private static final String[] JSON = { JSON2 };

	private StructuredMessage() {
	}

	static Object get(Message message) {
		if (message instanceof MultiFormatStringBuilderFormattable multiFormatMessage
				&& hasJsonFormat(multiFormatMessage)) {
			return WritableJson.of((out) -> formatTo(multiFormatMessage, out));
		}
		return message.getFormattedMessage();
	}

	private static boolean hasJsonFormat(MultiFormatStringBuilderFormattable message) {
		for (String format : message.getFormats()) {
			if (JSON2.equalsIgnoreCase(format)) {
				return true;
			}
		}
		return false;
	}

	private static void formatTo(MultiFormatStringBuilderFormattable message, Appendable out) throws IOException {
		if (out instanceof StringBuilder stringBuilder) {
			message.formatTo(JSON, stringBuilder);
		}
		else {
			out.append(message.getFormattedMessage(JSON));
		}
	}

}
