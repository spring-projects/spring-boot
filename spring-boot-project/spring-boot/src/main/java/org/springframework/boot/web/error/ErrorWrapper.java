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

package org.springframework.boot.web.error;

import jakarta.annotation.Nullable;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.util.Assert;

/**
 * A wrapper class for error objects that implements {@link MessageSourceResolvable}.
 * This class extends {@link DefaultMessageSourceResolvable} and delegates the
 * message resolution to the wrapped error object.
 *
 * @author Yongjun Hong
 * @since 3.5.0
 */
public class ErrorWrapper extends DefaultMessageSourceResolvable {

	private final Object error;

	/**
	 * Create a new {@code ErrorWrapper} instance with the specified error.
	 *
	 * @param error the error object to wrap (must not be {@code null})
	 */
	public ErrorWrapper(Object error) {
		this(error, null, null, null);
	}

	/**
	 * Create a new {@code ErrorWrapper} instance with the specified error, codes,
	 * arguments, and default message.
	 *
	 * @param error the error object to wrap (must not be {@code null})
	 * @param codes the codes to be used for message resolution
	 * @param arguments the arguments to be used for message resolution
	 * @param defaultMessage the default message to be used if no message is found
	 */
	public ErrorWrapper(Object error, @Nullable String[] codes, @Nullable Object[] arguments, @Nullable String defaultMessage) {
		super(codes, arguments, defaultMessage);
		Assert.notNull(error, "Error must not be null");
		this.error = error;
	}

	/**
	 * Return the codes to be used for message resolution.
	 *
	 * @return the codes to be used for message resolution
	 */
	@Override
	public String[] getCodes() {
		return ((MessageSourceResolvable) this.error).getCodes();
	}

	/**
	 * Return the arguments to be used for message resolution.
	 *
	 * @return the arguments to be used for message resolution
	 */
	@Override
	public Object[] getArguments() {
		return ((MessageSourceResolvable) this.error).getArguments();
	}

	/**
	 * Return the default message to be used if no message is found.
	 *
	 * @return the default message to be used if no message is found
	 */
	@Override
	public String getDefaultMessage() {
		return ((MessageSourceResolvable) this.error).getDefaultMessage();
	}

}
