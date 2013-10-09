/*
 * Copyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.loader;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

/**
 * Hamcrest matcher to tests that a byte array starts with specific bytes.
 * 
 * @author Phillip Webb
 */
public class ByteArrayStartsWith extends TypeSafeMatcher<byte[]> {

	private byte[] bytes;

	public ByteArrayStartsWith(byte[] bytes) {
		this.bytes = bytes;
	}

	@Override
	public void describeTo(Description description) {
		description.appendText("a byte array starting with ").appendValue(bytes);
	}

	@Override
	protected boolean matchesSafely(byte[] item) {
		if (item.length < bytes.length) {
			return false;
		}
		for (int i = 0; i < bytes.length; i++) {
			if (item[i] != bytes[i]) {
				return false;
			}
		}
		return true;
	}

}
