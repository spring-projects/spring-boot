/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.logging.structured;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.json.JsonWriter.MemberPath;
import org.springframework.boot.json.JsonWriter.Members;
import org.springframework.boot.util.Instantiator;
import org.springframework.util.CollectionUtils;

/**
 * {@link StructuredLoggingJsonMembersCustomizer} to apply
 * {@link StructuredLoggingJsonProperties}.
 *
 * @author Phillip Webb
 * @author Yanming Zhou
 */
class StructuredLoggingJsonPropertiesJsonMembersCustomizer implements StructuredLoggingJsonMembersCustomizer<Object> {

	private final Instantiator<?> instantiator;

	private final StructuredLoggingJsonProperties properties;

	private final Set<MemberPath> includedPaths;

	private final boolean nested;

	StructuredLoggingJsonPropertiesJsonMembersCustomizer(Instantiator<?> instantiator,
			StructuredLoggingJsonProperties properties, boolean nested) {
		this.instantiator = instantiator;
		this.properties = properties;
		this.includedPaths = toIncludedPaths(properties.include());
		this.nested = nested;
	}

	private static Set<MemberPath> toIncludedPaths(Set<String> include) {
		if (CollectionUtils.isEmpty(include)) {
			return Collections.emptySet();
		}
		return include.stream().map(MemberPath::of).collect(Collectors.toUnmodifiableSet());
	}

	@Override
	public void customize(Members<Object> members) {
		members.applyingPathFilter(this::filterPath);
		members.applyingNameProcessor(this::renameJsonMembers);
		Map<String, String> add = this.properties.add();
		if (!CollectionUtils.isEmpty(add)) {
			if (this.nested) {
				ContextPairs contextPairs = new ContextPairs(true, "");
				members.add().usingPairs(contextPairs.nested((pairs) -> pairs.addMapEntries((source) -> add)));
			}
			else {
				add.forEach(members::add);
			}
		}
		this.properties.customizers(this.instantiator).forEach((customizer) -> customizer.customize(members));
	}

	String renameJsonMembers(MemberPath path, String existingName) {
		Map<String, String> rename = this.properties.rename();
		String key = path.toUnescapedString();
		return !CollectionUtils.isEmpty(rename) ? rename.getOrDefault(key, existingName) : existingName;
	}

	boolean filterPath(MemberPath path) {
		boolean included = isIncluded(path);
		boolean excluded = !CollectionUtils.isEmpty(this.properties.exclude())
				&& this.properties.exclude().contains(path.toUnescapedString());
		return (!included || excluded);
	}

	private boolean isIncluded(MemberPath path) {
		if (this.includedPaths.isEmpty()) {
			return true;
		}
		for (MemberPath includedPath : this.includedPaths) {
			if (isSameOrAncestor(includedPath, path) || isSameOrAncestor(path, includedPath)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Return if the given {@code path} is the same as, or a descendant of, the given
	 * {@code candidate} path.
	 * @param candidate the candidate path
	 * @param path the path to test
	 * @return {@code true} if the path is the same as, or a descendant of, the candidate
	 */
	private static boolean isSameOrAncestor(MemberPath candidate, MemberPath path) {
		for (MemberPath current = path; current != null; current = current.parent()) {
			if (current.equals(candidate)) {
				return true;
			}
		}
		return false;
	}

}
