/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.context.embedded;

import javax.servlet.MultipartConfigElement;

/**
 * Factory that can be used to create a {@link MultipartConfigElement}. Size values can be
 * set using traditional {@literal long} values which are set in bytes or using more
 * readable {@literal String} variants that accept KB or MB suffixes, for example:
 *
 * <pre class="code">
 * factory.setMaxFileSize(&quot;10Mb&quot;);
 * factory.setMaxRequestSize(&quot;100Kb&quot;);
 * </pre>
 *
 * @author Phillip Webb
 * @deprecated as of 1.4 in favor of org.springframework.boot.web.MultipartConfigFactory
 */
@Deprecated
public class MultipartConfigFactory
		extends org.springframework.boot.web.servlet.MultipartConfigFactory {

}
