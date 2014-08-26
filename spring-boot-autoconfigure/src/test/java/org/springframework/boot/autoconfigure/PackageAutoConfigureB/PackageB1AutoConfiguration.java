package org.springframework.boot.autoconfigure.PackageAutoConfigureB;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.PackageAutoConfigureV.PackageV1AutoConfiguration;

/**
 * Test class for {@link AutoConfigurationSorter}.
 * 
 * @author David Liu
 * @since 1.1.4
 */

@AutoConfigureAfter(packages = { "org.springframework.boot.autoconfigure.PackageAutoConfigureC",
		"org.springframework.boot.autoconfigure.PackageAutoConfigureD",
		"org.springframework.boot.autoconfigure.PackageAutoConfigureE" }, value = PackageV1AutoConfiguration.class)
public class PackageB1AutoConfiguration {

}
