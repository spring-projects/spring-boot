/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.test.autoconfigure.xml;

import java.io.InputStream;
import java.io.Reader;

import org.springframework.boot.test.xml.AbstractXmlMarshalTester;
import org.springframework.core.ResolvableType;
import org.springframework.util.Assert;

/**
 * Example {@link AbstractXmlMarshalTester} used to check that XML testers are
 * initialized. The marshalling methods are never called, only the initialization applied
 * by {@link XmlTestersAutoConfiguration} is of interest.
 *
 * @param <T> the type under test
 * @author Tiziano Basile
 */
class ExampleXmlMarshalTester<T> extends AbstractXmlMarshalTester<T> {

	boolean isInitialized() {
		return getType() != null;
	}

	ResolvableType getTypeUnderTest() {
		ResolvableType type = getType();
		Assert.state(type != null, "Tester has not been initialized");
		return type;
	}

	@Override
	protected String writeObject(T value, ResolvableType type) {
		throw new UnsupportedOperationException("Not used by these tests");
	}

	@Override
	protected T readObject(InputStream inputStream, ResolvableType type) {
		throw new UnsupportedOperationException("Not used by these tests");
	}

	@Override
	protected T readObject(Reader reader, ResolvableType type) {
		throw new UnsupportedOperationException("Not used by these tests");
	}

}
