/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.logging.logback;

import ch.qos.logback.core.model.NamedModel;

/**
 * Logback {@link NamedModel model} to support {@code <springProfile>} tags.
 *
 * @author Andy Wilkinson
 * @see SpringProfileAction
 * @see SpringProfileModelHandler
 */
class SpringProfileModel extends NamedModel {

}
