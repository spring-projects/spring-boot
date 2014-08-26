package org.springframework.boot.autoconfigure.PackageAutoConfigureW;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.PackageAutoConfigureU.PackageU1AutoConfiguration;
import org.springframework.boot.autoconfigure.PackageAutoConfigureV.PackageV1AutoConfiguration;

/**
 * Test class for {@link AutoConfigurationSorter}.
 * 
 * @author David Liu
 * @since 1.1.4
 */

@AutoConfigureBefore(packages = "org.springframework.boot.autoconfigure.PackageAutoConfigureB", value = {
		PackageU1AutoConfiguration.class, PackageV1AutoConfiguration.class })
public class PackageW1AutoConfiguration {

}
