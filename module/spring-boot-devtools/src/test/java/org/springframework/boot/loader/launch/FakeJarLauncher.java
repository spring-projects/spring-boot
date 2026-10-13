/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.loader.launch;

import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

/**
 * Fake launcher in the {@code org.springframework.boot.loader.launch} package used in
 * {@code MainMethodTests}.
 *
 * @author Phillip Webb
 */
public final class FakeJarLauncher {

	public static @Nullable Consumer<String[]> action;

	private FakeJarLauncher() {
	}

	public static void main(String... args) {
		if (action != null) {
			action.accept(args);
		}
	}

}
