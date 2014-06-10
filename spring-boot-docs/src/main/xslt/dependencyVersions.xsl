<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
				xmlns:mvn="http://maven.apache.org/POM/4.0.0"
				version="1.0">

	<xsl:output method="text" encoding="UTF-8" indent="no"/>

	<xsl:template match="/">
		<xsl:text>|===&#xa;</xsl:text>
		<xsl:text>| Group ID | Artifact ID | Version&#xa;</xsl:text>
		<xsl:for-each select="//mvn:dependency">
			<xsl:sort select="mvn:groupId"/>
			<xsl:sort select="mvn:artifactId"/>
			<xsl:text>&#xa;</xsl:text>
			<xsl:text>| `</xsl:text>
			<xsl:copy-of select="mvn:groupId"/>
			<xsl:text>`&#xa;</xsl:text>
			<xsl:text>| `</xsl:text>
			<xsl:copy-of select="mvn:artifactId"/>
			<xsl:text>`&#xa;</xsl:text>
			<xsl:text>| </xsl:text>
			<xsl:copy-of select="mvn:version"/>
			<xsl:text>&#xa;</xsl:text>
		</xsl:for-each>
		<xsl:text>|===</xsl:text>
	</xsl:template>

</xsl:stylesheet>
