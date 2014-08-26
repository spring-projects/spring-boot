package org.springframework.boot.autoconfigure.PackageAutoConfigureY;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;

/**
 * Test class for {@link AutoConfigurationSorter}.
 * 
 * @author David Liu
 * @since 1.1.4
 */

@AutoConfigureBefore(packages = "org.springframework.boot.autoconfigure.PackageAutoConfigureX")
public class PackageY1AutoConfiguration {

}
