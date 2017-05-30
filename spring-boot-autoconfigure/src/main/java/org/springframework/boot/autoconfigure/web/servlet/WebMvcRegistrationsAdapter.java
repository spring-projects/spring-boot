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

package org.springframework.boot.autoconfigure.web.servlet;

/**
 * An implementation of {@link WebMvcRegistrations} with empty methods allowing
 * sub-classes to override only the methods they're interested in.
 *
 * @author Brian Clozel
 * @since 1.4.0
 * @deprecated as of 2.0.0 {@link WebMvcRegistrations} has default methods (made possible
 * by a Java 8 baseline) and can be implemented directly without the need for this adapter
 */
@Deprecated
public class WebMvcRegistrationsAdapter implements WebMvcRegistrations {

}
