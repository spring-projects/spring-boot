/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.condition;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Outcome for a condition match, including log message.
 *
 * @author Phillip Webb
 * @since 1.0.0
 * @see ConditionMessage
 */
public class ConditionOutcome {

	private final boolean match;

	private final ConditionMessage message;

	/**
	 * Create a new {@link ConditionOutcome} instance. For more consistent messages
	 * consider using {@link #ConditionOutcome(boolean, ConditionMessage)}.
	 * @param match if the condition is a match
	 * @param message the condition message
	 */
	public ConditionOutcome(boolean match, String message) {
		this(match, ConditionMessage.of(message));
	}

	/**
	 * Create a new {@link ConditionOutcome} instance.
	 * @param match if the condition is a match
	 * @param message the condition message
	 */
	public ConditionOutcome(boolean match, ConditionMessage message) {
		Assert.notNull(message, "'message' must not be null");
		this.match = match;
		this.message = message;
	}

	/**
	 * Create a new {@link ConditionOutcome} instance for a 'match'.
	 * @return the {@link ConditionOutcome}
	 */
	public static ConditionOutcome match() {
		return match(ConditionMessage.empty());
	}

	/**
	 * Create a new {@link ConditionOutcome} instance for 'match'. For more consistent
	 * messages consider using {@link #match(ConditionMessage)}.
	 * @param message the message
	 * @return the {@link ConditionOutcome}
	 */
	public static ConditionOutcome match(String message) {
		return new ConditionOutcome(true, message);
	}

	/**
	 * Create a new {@link ConditionOutcome} instance for 'match'.
	 * @param message the message
	 * @return the {@link ConditionOutcome}
	 */
	public static ConditionOutcome match(ConditionMessage message) {
		return new ConditionOutcome(true, message);
	}

	/**
	 * Create a new {@link ConditionOutcome} instance for 'no match'. For more consistent
	 * messages consider using {@link #noMatch(ConditionMessage)}.
	 * @param message the message
	 * @return the {@link ConditionOutcome}
	 */
	public static ConditionOutcome noMatch(String message) {
		return new ConditionOutcome(false, message);
	}

	/**
	 * Create a new {@link ConditionOutcome} instance for 'no match'.
	 * @param message the message
	 * @return the {@link ConditionOutcome}
	 */
	public static ConditionOutcome noMatch(ConditionMessage message) {
		return new ConditionOutcome(false, message);
	}

	/**
	 * Return {@code true} if the outcome was a match.
	 * @return {@code true} if the outcome matches
	 */
	public boolean isMatch() {
		return this.match;
	}

	/**
	 * Return an outcome message or {@code null}.
	 * @return the message or {@code null}
	 */
	public @Nullable String getMessage() {
		return this.message.isEmpty() ? null : this.message.toString();
	}

	/**
	 * Return an outcome message.
	 * @return the message
	 */
	public ConditionMessage getConditionMessage() {
		return this.message;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() == obj.getClass()) {
			ConditionOutcome other = (ConditionOutcome) obj;
			return (this.match == other.match && ObjectUtils.nullSafeEquals(this.message, other.message));
		}
		return super.equals(obj);
	}

	@Override
	public int hashCode() {
		return Boolean.hashCode(this.match) * 31 + ObjectUtils.nullSafeHashCode(this.message);
	}

	@Override
	public String toString() {
		return (this.message != null) ? this.message.toString() : "";
	}

}
