/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.logging;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;

/**
 * Logger groups configured through the Spring Environment.
 *
 * @author HaiTao Zhang
 * @author Phillip Webb
 * @since 2.2.0
 * @see LoggerGroup
 */
public final class LoggerGroups implements Iterable<LoggerGroup> {

	private final Map<String, LoggerGroup> groups = new ConcurrentHashMap<>();

	public LoggerGroups() {
	}

	public LoggerGroups(Map<String, List<String>> namesAndMembers) {
		putAll(namesAndMembers);
	}

	public void putAll(Map<String, List<String>> namesAndMembers) {
		namesAndMembers.forEach(this::put);
	}

	private void put(String name, List<String> members) {
		put(new LoggerGroup(name, members));
	}

	private void put(LoggerGroup loggerGroup) {
		this.groups.put(loggerGroup.getName(), loggerGroup);
	}

	public @Nullable LoggerGroup get(String name) {
		return this.groups.get(name);
	}

	@Override
	public Iterator<LoggerGroup> iterator() {
		return this.groups.values().iterator();
	}

}
