/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.web.embedded.tomcat;

import org.apache.catalina.Context;
import org.apache.catalina.core.StandardContext;

/**
 * A {@link TomcatContextCustomizer} that disables Tomcat's reflective reference clearing
 * to avoid reflective access warnings on Java 9 and later JVMs.
 *
 * @author Andy Wilkinson
 */
class DisableReferenceClearingContextCustomizer implements TomcatContextCustomizer {

	@Override
	public void customize(Context context) {
		if (!(context instanceof StandardContext standardContext)) {
			return;
		}
		try {
			standardContext.setClearReferencesObjectStreamClassCaches(false);
			standardContext.setClearReferencesRmiTargets(false);
			standardContext.setClearReferencesThreadLocals(false);
		}
		catch (NoSuchMethodError ex) {
			// Earlier version of Tomcat (probably without
			// setClearReferencesThreadLocals). Continue.
		}
	}

}
