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

package org.springframework.boot.env;

import java.io.File;

/**
 * Interface that uniquely represents the origin of a property. For example, a property
 * loaded from a {@link File} may have an origin made up of the file name along with
 * line/column numbers.
 * <p>
 * Implementations must provide sensible {@code hashCode()}, {@code equals(...)} and
 * {@code #toString()} implementations.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @since 2.0.0
 */
public interface PropertyOrigin {

}
