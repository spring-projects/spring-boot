/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.origin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Interface that uniquely represents the origin of an item. For example, an item loaded
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
 * @see TextResourceOrigin
 */
public interface Origin {

	/**
	 * Return the parent origin for this instance if there is one. The parent origin
	 * provides the origin of the item that created this one.
	 * @return the parent origin or {@code null}
	 * @since 2.4.0
	 * @see Origin#parentsFrom(Object)
	 */
	default Origin getParent() {
		return null;
	}

	/**
	 * Find the {@link Origin} that an object originated from. Checks if the source object
	 * is an {@link Origin} or {@link OriginProvider} and also searches exception stacks.
	 * @param source the source object or {@code null}
	 * @return an optional {@link Origin}
	 */
	static Origin from(Object source) {
		if (source instanceof Origin) {
			return (Origin) source;
		}
		Origin origin = null;
		if (source instanceof OriginProvider) {
			origin = ((OriginProvider) source).getOrigin();
		}
		if (origin == null && source instanceof Throwable) {
			return from(((Throwable) source).getCause());
		}
		return origin;
	}

	/**
	 * Find the parents of the {@link Origin} that an object originated from. Checks if
	 * the source object is an {@link Origin} or {@link OriginProvider} and also searches
	 * exception stacks. Provides a list of all parents up to root {@link Origin},
	 * starting with the most immediate parent.
	 * @param source the source object or {@code null}
	 * @return a list of parents or an empty list if the source is {@code null}, has no
	 * origin, or no parent
	 */
	static List<Origin> parentsFrom(Object source) {
		Origin origin = from(source);
		if (origin == null) {
			return Collections.emptyList();
		}
		Set<Origin> parents = new LinkedHashSet<>();
		origin = origin.getParent();
		while (origin != null && !parents.contains(origin)) {
			parents.add(origin);
			origin = origin.getParent();
		}
		return Collections.unmodifiableList(new ArrayList<>(parents));
	}

}
