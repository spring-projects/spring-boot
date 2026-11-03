/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.loader.net.protocol.jar;

/**
 * {@link ThreadLocal} state for {@link Handler} optimizations.
 *
 * @author Phillip Webb
 */
final class Optimizations {

	private static final class Node {

		final boolean readContents;

		final Node previous;

		Node(boolean readContents, Node previous) {
			this.readContents = readContents;
			this.previous = previous;
		}

	}

	private static final ThreadLocal<Node> status = new ThreadLocal<>();

	private Optimizations() {
	}

	static void enable(boolean readContents) {
		status.set(new Node(readContents, status.get()));
	}

	static void disable() {
		Node current = status.get();
		if (current != null) {
			if (current.previous != null) {
				status.set(current.previous);
			}
			else {
				status.remove();
			}
		}
	}

	static boolean isEnabled() {
		return status.get() != null;
	}

	static boolean isEnabled(boolean readContents) {
		Node current = status.get();
		return current != null && current.readContents == readContents;
	}

}
