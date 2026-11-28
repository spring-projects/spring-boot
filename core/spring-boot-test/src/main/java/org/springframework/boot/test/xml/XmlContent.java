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

package org.springframework.boot.test.xml;

import org.assertj.core.api.AssertProvider;
import org.jspecify.annotations.Nullable;

import org.springframework.core.ResolvableType;
import org.springframework.util.Assert;

/**
 * XML content usually created from an XML tester. Generally used only to
 * {@link AssertProvider provide} {@link XmlContentAssert} to AssertJ {@code assertThat}
 * calls.
 *
 * @param <T> the source type that created the content
 * @author Tiziano Basile
 * @since 4.2.0
 */
public final class XmlContent<T> implements AssertProvider<XmlContentAssert> {

	private final Class<?> resourceLoadClass;

	private final @Nullable ResolvableType type;

	private final String xml;

	/**
	 * Create a new {@link XmlContent} instance.
	 * @param resourceLoadClass the source class used to load resources
	 * @param type the type under test (or {@code null} if not known)
	 * @param xml the actual XML content
	 */
	public XmlContent(Class<?> resourceLoadClass, @Nullable ResolvableType type, String xml) {
		Assert.notNull(resourceLoadClass, "'resourceLoadClass' must not be null");
		Assert.notNull(xml, "'xml' must not be null");
		this.resourceLoadClass = resourceLoadClass;
		this.type = type;
		this.xml = xml;
	}

	/**
	 * Return the {@link XmlContentAssert} for this content. This method is the
	 * {@link AssertProvider} hook used by AssertJ and is not intended to be called
	 * directly. Use AssertJ's
	 * {@link org.assertj.core.api.Assertions#assertThat(AssertProvider) assertThat}
	 * instead.
	 * @return the assertion object
	 */
	@Override
	public XmlContentAssert assertThat() {
		return new XmlContentAssert(this.resourceLoadClass, this.xml);
	}

	/**
	 * Return the actual XML content string.
	 * @return the XML content
	 */
	public String getXml() {
		return this.xml;
	}

	@Override
	public String toString() {
		String createdFrom = (this.type != null) ? " created from " + this.type : "";
		return "XmlContent " + this.xml + createdFrom;
	}

}
