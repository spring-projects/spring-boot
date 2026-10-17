/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.jackson2.only;

import org.jspecify.annotations.Nullable;

/**
 * POJO for serialization to JSON.
 *
 * @author Andy Wilkinson
 */
class JsonPojo {

	private @Nullable String alpha;

	private @Nullable String bravo;

	@Nullable String getAlpha() {
		return this.alpha;
	}

	void setAlpha(@Nullable String alpha) {
		this.alpha = alpha;
	}

	@Nullable String getBravo() {
		return this.bravo;
	}

	void setBravo(@Nullable String bravo) {
		this.bravo = bravo;
	}

}
