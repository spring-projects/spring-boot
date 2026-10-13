/*
 * Copyright 2012-present the original author or authors.
 */
package org.springframework.boot.docs.features.ssl.bundles

import org.springframework.boot.ssl.SslBundles
import org.springframework.stereotype.Component

@Component
@Suppress("UNUSED_VARIABLE")
class MyComponent(sslBundles: SslBundles) {

    init {
        val sslBundle = sslBundles.getBundle("mybundle")
        val sslContext = sslBundle.createSslContext()
        // do something with the created sslContext
    }

}

