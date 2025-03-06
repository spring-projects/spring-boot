/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.jms;

import java.util.HashMap;
import java.util.Map;

import jakarta.jms.Session;

import org.springframework.jms.support.JmsAccessor;

/**
 * Acknowledge modes for a JMS Session. Supports the acknowledge modes defined by
 * {@link jakarta.jms.Session} as well as other, non-standard modes.
 *
 * <p>
 * Note that {@link jakarta.jms.Session#SESSION_TRANSACTED} is not defined. It should be
 * handled through a call to {@link JmsAccessor#setSessionTransacted(boolean)}.
 *
 * @author Andy Wilkinson
 * @since 3.2.0
 */
public final class AcknowledgeMode {

	private static final Map<String, AcknowledgeMode> knownModes = new HashMap<>(3);

	/**
	 * Messages sent or received from the session are automatically acknowledged. This is
	 * the simplest mode and enables once-only message delivery guarantee.
	 */
	public static final AcknowledgeMode AUTO = new AcknowledgeMode(Session.AUTO_ACKNOWLEDGE);

	/**
	 * Messages are acknowledged once the message listener implementation has called
	 * {@link jakarta.jms.Message#acknowledge()}. This mode gives the application (rather
	 * than the JMS provider) complete control over message acknowledgement.
	 */
	public static final AcknowledgeMode CLIENT = new AcknowledgeMode(Session.CLIENT_ACKNOWLEDGE);

	/**
	 * Similar to auto acknowledgment except that said acknowledgment is lazy. As a
	 * consequence, the messages might be delivered more than once. This mode enables
	 * at-least-once message delivery guarantee.
	 */
	public static final AcknowledgeMode DUPS_OK = new AcknowledgeMode(Session.DUPS_OK_ACKNOWLEDGE);

	static {
		knownModes.put("auto", AUTO);
		knownModes.put("client", CLIENT);
		knownModes.put("dupsok", DUPS_OK);
	}

	private final int mode;

	private AcknowledgeMode(int mode) {
		this.mode = mode;
	}

	public int getMode() {
		return this.mode;
	}

	/**
	 * Creates an {@code AcknowledgeMode} of the given {@code mode}. The mode may be
	 * {@code auto}, {@code client}, {@code dupsok} or a non-standard acknowledge mode
	 * that can be {@link Integer#parseInt parsed as an integer}.
	 * @param mode the mode
	 * @return the acknowledge mode
	 */
	public static AcknowledgeMode of(String mode) {
		String canonicalMode = canonicalize(mode);
		AcknowledgeMode knownMode = knownModes.get(canonicalMode);
		try {
			return (knownMode != null) ? knownMode : new AcknowledgeMode(Integer.parseInt(canonicalMode));
		}
		catch (NumberFormatException ex) {
			throw new IllegalArgumentException("'" + mode
					+ "' is neither a known acknowledge mode (auto, client, or dups_ok) nor an integer value");
		}
	}

	private static String canonicalize(String input) {
		StringBuilder canonicalName = new StringBuilder(input.length());
		input.chars()
			.filter(Character::isLetterOrDigit)
			.map(Character::toLowerCase)
			.forEach((c) -> canonicalName.append((char) c));
		return canonicalName.toString();
	}

}
