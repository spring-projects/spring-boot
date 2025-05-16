/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.web.error;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.springframework.context.MessageSourceResolvable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * A wrapper class for {@link MessageSourceResolvable} errors that is safe for JSON
 * serialization.
 *
 * @author Yongjun Hong
 * @author Phillip Webb
 * @since 3.5.0
 */
public final class Error implements MessageSourceResolvable {

	private final MessageSourceResolvable cause;

	/**
	 * Create a new {@code Error} instance with the specified cause.
	 * @param cause the error cause (must not be {@code null})
	 */
	private Error(MessageSourceResolvable cause) {
		Assert.notNull(cause, "'cause' must not be null");
		this.cause = cause;
	}

	@Override
	public String[] getCodes() {
		return this.cause.getCodes();
	}

	@Override
	public Object[] getArguments() {
		return this.cause.getArguments();
	}

	@Override
	public String getDefaultMessage() {
		return this.cause.getDefaultMessage();
	}

	/**
	 * Return the original cause of the error.
	 * @return the error cause
	 */
	public MessageSourceResolvable getCause() {
		return this.cause;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		return Objects.equals(this.cause, ((Error) obj).cause);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.cause);
	}

	@Override
	public String toString() {
		return this.cause.toString();
	}

	/**
	 * Wrap the given errors.
	 * @param errors the errors to wrap
	 * @return a new Error list
	 */
	public static List<Error> wrap(List<? extends MessageSourceResolvable> errors) {
		if (CollectionUtils.isEmpty(errors)) {
			return Collections.emptyList();
		}
		List<Error> result = new ArrayList<>(errors.size());
		for (MessageSourceResolvable error : errors) {
			result.add(new Error(error));
		}
		return List.copyOf(result);
	}

}
