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

package org.springframework.boot.configurationdocs;

import java.util.Objects;

/**
 * Abstract class for entries in {@link ConfigurationTable}.
 *
 * @author Brian Clozel
 */
abstract class AbstractConfigurationEntry
		implements Comparable<AbstractConfigurationEntry> {

	protected static final String NEWLINE = System.lineSeparator();

	protected String key;

	public String getKey() {
		return this.key;
	}

	public abstract void writeAsciidoc(StringBuilder builder);

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		AbstractConfigurationEntry that = (AbstractConfigurationEntry) o;
		return this.key.equals(that.key);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.key);
	}

	@Override
	public int compareTo(AbstractConfigurationEntry other) {
		return this.key.compareTo(other.getKey());
	}

}
