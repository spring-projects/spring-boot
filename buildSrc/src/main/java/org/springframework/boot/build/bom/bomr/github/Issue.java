/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.bom.bomr.github;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.web.client.RestClient;

/**
 * Minimal representation of a GitHub issue.
 *
 * @author Andy Wilkinson
 */
public class Issue {

	private final RestClient rest;

	private final int number;

	private final String title;

	private final State state;

	Issue(RestClient rest, int number, String title, State state) {
		this.rest = rest;
		this.number = number;
		this.title = title;
		this.state = state;
	}

	public int getNumber() {
		return this.number;
	}

	public String getTitle() {
		return this.title;
	}

	public State getState() {
		return this.state;
	}

	/**
	 * Labels the issue with the given {@code labels}. Any existing labels are removed.
	 * @param labels the labels to apply to the issue
	 */
	public void label(List<String> labels) {
		Map<String, List<String>> body = Collections.singletonMap("labels", labels);
		this.rest.put().uri("issues/" + this.number + "/labels").body(body).retrieve().toBodilessEntity();
	}

	public enum State {

		/**
		 * The issue is open.
		 */
		OPEN,

		/**
		 * The issue is closed.
		 */
		CLOSED;

		static State of(String state) {
			if ("open".equals(state)) {
				return OPEN;
			}
			if ("closed".equals(state)) {
				return CLOSED;
			}
			else {
				throw new IllegalArgumentException("Unknown state '" + state + "'");
			}
		}

	}

}
