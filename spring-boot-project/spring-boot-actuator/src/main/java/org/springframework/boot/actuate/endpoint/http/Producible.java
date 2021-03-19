/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.http;

import org.springframework.util.MimeType;

/**
 * Interface to be implemented by an {@link Enum} that can be injected into an operation
 * on a web endpoint. The value of the {@code Producible} enum is resolved using the
 * {@code Accept} header of the request. When multiple values are equally acceptable, the
 * value with the highest {@link Enum#ordinal() ordinal} is used.
 *
 * @param <E> enum type that implements this interface
 * @author Andy Wilkinson
 * @since 2.5.0
 */
public interface Producible<E extends Enum<E> & Producible<E>> {

	/**
	 * Mime type that can be produced.
	 * @return the producible mime type
	 */
	MimeType getMimeType();

}
