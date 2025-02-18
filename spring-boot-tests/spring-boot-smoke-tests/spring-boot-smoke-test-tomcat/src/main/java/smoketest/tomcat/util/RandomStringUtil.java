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

package smoketest.tomcat.util;

import java.util.Base64;
import java.util.Random;

public final class RandomStringUtil {

	private RandomStringUtil() {
	}

	public static String getRandomBase64EncodedString(int length) {
		byte[] responseHeader = new byte[length];
		new Random().nextBytes(responseHeader);
		return Base64.getEncoder().encodeToString(responseHeader);
	}

}
