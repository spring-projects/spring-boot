package org.springframework.boot.autoconfigure.PackageAutoConfigureA;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.PackageAutoConfigureV.PackageV1AutoConfiguration;

/**
 * Test class for {@link AutoConfigurationSorter}.
 * 
 * @author David Liu
 * @since 1.1.4
 */

@AutoConfigureAfter(packages = "org.springframework.boot.autoconfigure.PackageAutoConfigureB", value = PackageV1AutoConfiguration.class)
public class PackageA1AutoConfiguration {

}
