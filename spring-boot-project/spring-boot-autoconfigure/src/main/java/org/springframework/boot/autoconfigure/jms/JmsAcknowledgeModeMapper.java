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

/**
 * Helper class used to map JMS acknowledge modes.
 *
 * @author Vedran Pavic
 */
final class JmsAcknowledgeModeMapper {

	private static final Map<String, Integer> acknowledgeModes = new HashMap<>(3);

	static {
		acknowledgeModes.put("auto", Session.AUTO_ACKNOWLEDGE);
		acknowledgeModes.put("client", Session.CLIENT_ACKNOWLEDGE);
		acknowledgeModes.put("dups_ok", Session.DUPS_OK_ACKNOWLEDGE);
	}

	private JmsAcknowledgeModeMapper() {
	}

	static int map(String acknowledgeMode) {
		return acknowledgeModes.computeIfAbsent(acknowledgeMode.toLowerCase(), Integer::parseInt);
	}

}
