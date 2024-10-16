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

package org.springframework.boot.logging.structured;

import java.util.Map;

import org.springframework.boot.json.JsonWriter.MemberPath;
import org.springframework.boot.json.JsonWriter.Members;
import org.springframework.boot.util.Instantiator;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * {@link StructureLoggingJsonMembersCustomizer} to apply
 * {@link StructuredLoggingJsonProperties}.
 *
 * @author Phillip Webb
 */
class StructuredLoggingJsonPropertiesJsonMembersCustomizer implements StructureLoggingJsonMembersCustomizer<Object> {

	private final Instantiator<?> instantiator;

	private final StructuredLoggingJsonProperties properties;

	StructuredLoggingJsonPropertiesJsonMembersCustomizer(Instantiator<?> instantiator,
			StructuredLoggingJsonProperties properties) {
		this.instantiator = instantiator;
		this.properties = properties;
	}

	@Override
	public void customize(Members<Object> members) {
		members.applyingPathFilter(this::filterPath);
		members.applyingNameProcessor(this::renameJsonMembers);
		Map<String, String> add = this.properties.add();
		if (!CollectionUtils.isEmpty(add)) {
			add.forEach(members::add);
		}
		String customizer = this.properties.customizer();
		if (StringUtils.hasLength(customizer)) {
			createAndApplyCustomizer(members, customizer);
		}
	}

	String renameJsonMembers(MemberPath path, String existingName) {
		Map<String, String> rename = this.properties.rename();
		String key = path.toUnescapedString();
		return !CollectionUtils.isEmpty(rename) ? rename.getOrDefault(key, existingName) : existingName;
	}

	boolean filterPath(MemberPath path) {
		boolean included = CollectionUtils.isEmpty(this.properties.include())
				|| this.properties.include().contains(path.toUnescapedString());
		boolean excluded = !CollectionUtils.isEmpty(this.properties.exclude())
				&& this.properties.exclude().contains(path.toUnescapedString());
		return (!included || excluded);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void createAndApplyCustomizer(Members<Object> members, String customizerClassName) {
		((StructureLoggingJsonMembersCustomizer) this.instantiator.instantiate(customizerClassName)).customize(members);
	}

}
