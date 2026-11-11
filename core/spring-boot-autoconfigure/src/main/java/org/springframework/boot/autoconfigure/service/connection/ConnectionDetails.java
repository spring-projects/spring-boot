/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.service.connection;

import org.springframework.boot.origin.OriginProvider;

/**
 * Base interface for types that provide the details required to establish a connection to
 * a remote service.
 * <p>
 * Implementation classes can also implement {@link OriginProvider} in order to provide
 * origin information.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 3.1.0
 */
public interface ConnectionDetails {

}
