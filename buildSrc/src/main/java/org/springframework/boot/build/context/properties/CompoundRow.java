/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.build.context.properties;

import java.util.Set;
import java.util.TreeSet;

/**
 * Table row regrouping a list of configuration properties sharing the same description.
 *
 * @author Brian Clozel
 * @author Phillip Webb
 */
class CompoundRow extends Row {

	private final Set<String> propertyNames;

	private final String description;

	CompoundRow(Snippet snippet, String prefix, String description) {
		super(snippet, prefix);
		this.description = description;
		this.propertyNames = new TreeSet<>();
	}

	void addProperty(ConfigurationProperty property) {
		this.propertyNames.add(property.getDisplayName());
	}

	@Override
	void write(Asciidoc asciidoc) {
		asciidoc.append("|");
		asciidoc.append("[[" + getAnchor() + "]]");
		asciidoc.append("<<" + getAnchor() + ",");
		this.propertyNames.forEach(asciidoc::appendWithHardLineBreaks);
		asciidoc.appendln(">>");
		asciidoc.appendln("|+++", this.description, "+++");
		asciidoc.appendln("|");
	}

}
