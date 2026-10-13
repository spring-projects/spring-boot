/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.tomcat;

import org.apache.catalina.Context;
import org.apache.catalina.core.StandardContext;

/**
 * A {@link TomcatContextCustomizer} that disables Tomcat's reflective reference clearing
 * to avoid reflective access warnings on Java 9 and later JVMs.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
public class DisableReferenceClearingContextCustomizer implements TomcatContextCustomizer {

	@Override
	public void customize(Context context) {
		if (!(context instanceof StandardContext standardContext)) {
			return;
		}
		standardContext.setClearReferencesRmiTargets(false);
		standardContext.setClearReferencesThreadLocals(false);
	}

}
