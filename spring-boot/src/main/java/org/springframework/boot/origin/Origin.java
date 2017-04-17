/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.origin;

import java.io.File;

/**
 * Interface that uniquely represents the origin of an item. For example, a item loaded
 * from a {@link File} may have an origin made up of the file name along with line/column
 * numbers.
 * <p>
 * Implementations must provide sensible {@code hashCode()}, {@code equals(...)} and
 * {@code #toString()} implementations.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @since 2.0.0
 * @see OriginProvider
 */
public interface Origin {

	/**
	 * Find the {@link Origin} that an object originated from. Checks if the source object
	 * is a {@link OriginProvider} and also searches exception stacks.
	 * @param source the source object or {@code null}
	 * @return an optional {@link Origin}
	 */
	static Origin from(Object source) {
		if (source instanceof Origin) {
			return (Origin) source;
		}
		Origin origin = null;
		if (source != null && source instanceof OriginProvider) {
			origin = ((OriginProvider) source).getOrigin();
		}
		if (origin == null && source != null && source instanceof Throwable) {
			return from(((Throwable) source).getCause());
		}
		return origin;
	}

}
