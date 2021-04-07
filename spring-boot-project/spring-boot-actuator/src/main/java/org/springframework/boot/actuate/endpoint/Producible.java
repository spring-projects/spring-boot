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

package org.springframework.boot.actuate.endpoint;

import org.springframework.boot.actuate.endpoint.annotation.DeleteOperation;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.util.MimeType;

/**
 * Interface that can be implemented by any {@link Enum} that represents a finite set of
 * producible mime-types.
 * <p>
 * Can be used with {@link ReadOperation @ReadOperation},
 * {@link WriteOperation @ReadOperation} and {@link DeleteOperation @ReadOperation}
 * annotations to quickly define a list of {@code produces} values.
 * <p>
 * {@link Producible} types can also be injected into operations when the underlying
 * technology supports content negotiation. For example, with web based endpoints, the
 * value of the {@code Producible} enum is resolved using the {@code Accept} header of the
 * request. When multiple values are equally acceptable, the value with the highest
 * {@link Enum#ordinal() ordinal} is used.
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
	MimeType getProducedMimeType();

}
