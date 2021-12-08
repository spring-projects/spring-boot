package org.springframework.boot.docs.configurationmetadata.manualhints.valuehint

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("my")
class MyProperties(val contexts: Map<String, Int>) {
}