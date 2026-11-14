/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.logging.structured;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

	private final Set<String> includedPaths;

	private final Set<String> includedPathAncestors;

	private final boolean nested;

	StructuredLoggingJsonPropertiesJsonMembersCustomizer(Instantiator<?> instantiator,
			StructuredLoggingJsonProperties properties, boolean nested) {
		this.instantiator = instantiator;
		this.properties = properties;
		this.includedPaths = (!CollectionUtils.isEmpty(properties.include())) ? properties.include()
				: Collections.emptySet();
		this.includedPathAncestors = ancestorsOf(this.includedPaths);
		this.nested = nested;
	}

	/**
	 * Return the paths of all the ancestors of the given {@code paths}. Ancestors are
	 * retained so that an included path can be reached.
	 * @param paths the paths
	 * @return the ancestor paths
	 */
	private static Set<String> ancestorsOf(Set<String> paths) {
		Set<String> ancestors = new HashSet<>();
		for (String path : paths) {
			for (int i = path.length() - 1; i > 0; i--) {
				char ch = path.charAt(i);
				if (ch == '.' || ch == '[') {
					ancestors.add(path.substring(0, i));
				}
			}
		}
		return Collections.unmodifiableSet(ancestors);
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
		String name = path.toUnescapedString();
		boolean included = isIncluded(name);
		boolean excluded = !CollectionUtils.isEmpty(this.properties.exclude())
				&& this.properties.exclude().contains(name);
		return (!included || excluded);
	}

	private boolean isIncluded(String path) {
		if (this.includedPaths.isEmpty()) {
			return true;
		}
		return this.includedPaths.contains(path) || this.includedPathAncestors.contains(path)
				|| isDescendantOfIncludedPath(path);
	}

	private boolean isDescendantOfIncludedPath(String path) {
		for (String includedPath : this.includedPaths) {
			if (isDescendantOf(includedPath, path)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Return if the given {@code path} is a descendant of the given {@code candidate}
	 * path. A path is only a descendant when it continues with a {@code '.'} or a
	 * {@code '['} immediately after the candidate, so that an include of {@code x.y} does
	 * not include a member named {@code x.yy}.
	 * @param candidate the candidate ancestor path
	 * @param path the path to test
	 * @return {@code true} if {@code path} is a descendant of {@code candidate}
	 */
	private static boolean isDescendantOf(String candidate, String path) {
		if (path.length() <= candidate.length() || !path.startsWith(candidate)) {
			return false;
		}
		char next = path.charAt(candidate.length());
		return next == '.' || next == '[';
	}

}
