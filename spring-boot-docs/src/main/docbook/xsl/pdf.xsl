<?xml version="1.0" encoding="UTF-8"?>

<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
				xmlns:d="http://docbook.org/ns/docbook"
				xmlns:fo="http://www.w3.org/1999/XSL/Format"
				xmlns:xslthl="http://xslthl.sf.net"
				xmlns:xlink='http://www.w3.org/1999/xlink'
				xmlns:exsl="http://exslt.org/common"
				exclude-result-prefixes="exsl xslthl d xlink"
				version='1.0'>

	<xsl:import href="urn:docbkx:stylesheet"/>
	<xsl:import href="urn:docbkx:stylesheet/highlight.xsl"/>
	<xsl:import href="common.xsl"/>

	<!-- Extensions -->
	<xsl:param name="fop1.extensions" select="1"/>

	<xsl:param name="paper.type" select="'A4'"/>
	<xsl:param name="page.margin.top" select="'1cm'"/>
	<xsl:param name="region.before.extent" select="'1cm'"/>
	<xsl:param name="body.margin.top" select="'1.5cm'"/>

	<xsl:param name="body.margin.bottom" select="'1.5cm'"/>
	<xsl:param name="region.after.extent" select="'1cm'"/>
	<xsl:param name="page.margin.bottom" select="'1cm'"/>
	<xsl:param name="title.margin.left" select="'0cm'"/>

	<!-- allow break across pages -->
	<xsl:attribute-set name="formal.object.properties">
		<xsl:attribute name="keep-together.within-column">auto</xsl:attribute>
	</xsl:attribute-set>

	<!-- use color links and sensible rendering -->
	<xsl:attribute-set name="xref.properties">
		<xsl:attribute name="text-decoration">underline</xsl:attribute>
		<xsl:attribute name="color">#204060</xsl:attribute>
	</xsl:attribute-set>
	<xsl:param name="ulink.show" select="0"></xsl:param>
	<xsl:param name="ulink.footnotes" select="0"></xsl:param>

	<!-- TITLE PAGE -->

	<xsl:template name="book.titlepage.recto">
		<fo:block>
			<fo:table table-layout="fixed" width="175mm">
				<fo:table-column column-width="175mm"/>
				<fo:table-body>
					<fo:table-row>
						<fo:table-cell text-align="center">
							<fo:block>
								<fo:external-graphic src="images/logo.png" width="240px"
									height="auto" content-width="scale-to-fit"
									content-height="scale-to-fit"
									content-type="content-type:image/png" text-align="center"
								/>
							</fo:block>
							<fo:block font-family="Helvetica" font-size="20pt" font-weight="bold" padding="10mm">
								<xsl:value-of select="d:info/d:title"/>
							</fo:block>
							<fo:block font-family="Helvetica" font-size="14pt" padding-before="2mm">
								<xsl:value-of select="d:info/d:subtitle"/>
							</fo:block>
							<fo:block font-family="Helvetica" font-size="14pt" padding="2mm">
								<xsl:value-of select="d:info/d:releaseinfo"/>
							</fo:block>
						</fo:table-cell>
					</fo:table-row>
					<fo:table-row>
						<fo:table-cell text-align="center">
							<fo:block font-family="Helvetica" font-size="14pt" padding="5mm">
								<xsl:value-of select="d:info/d:pubdate"/>
							</fo:block>
						</fo:table-cell>
					</fo:table-row>
					<fo:table-row>
						<fo:table-cell text-align="center">
							<fo:block font-family="Helvetica" font-size="10pt" padding="10mm">
								<xsl:for-each select="d:info/d:authorgroup/d:author">
									<xsl:if test="position() > 1">
										<xsl:text>, </xsl:text>
									</xsl:if>
									<xsl:value-of select="."/>
								</xsl:for-each>
							</fo:block>

							<fo:block font-family="Helvetica" font-size="10pt" padding="5mm">
								<xsl:value-of select="d:info/d:pubdate"/>
							</fo:block>

							<fo:block font-family="Helvetica" font-size="10pt" padding="5mm" padding-before="25em">
								<xsl:text>Copyright &#xA9; </xsl:text><xsl:value-of select="d:info/d:copyright"/>
							</fo:block>

							<fo:block font-family="Helvetica" font-size="8pt" padding="1mm">
								<xsl:value-of select="d:info/d:legalnotice"/>
							</fo:block>
						</fo:table-cell>
					</fo:table-row>
				</fo:table-body>
			</fo:table>
		</fo:block>
	</xsl:template>

	<!-- Prevent blank pages in output -->
	<xsl:template name="book.titlepage.before.verso">
	</xsl:template>
	<xsl:template name="book.titlepage.verso">
	</xsl:template>
	<xsl:template name="book.titlepage.separator">
	</xsl:template>

	<!--  HEADER -->

	<!-- More space in the center header for long text -->
	<xsl:attribute-set name="header.content.properties">
		<xsl:attribute name="font-family">
			<xsl:value-of select="$body.font.family"/>
		</xsl:attribute>
		<xsl:attribute name="margin-left">-5em</xsl:attribute>
		<xsl:attribute name="margin-right">-5em</xsl:attribute>
		<xsl:attribute name="font-size">8pt</xsl:attribute>
	</xsl:attribute-set>

	<xsl:template name="header.content">
		<xsl:param name="pageclass" select="''"/>
		<xsl:param name="sequence" select="''"/>
		<xsl:param name="position" select="''"/>
		<xsl:param name="gentext-key" select="''"/>

		<xsl:variable name="Version">
			<xsl:choose>
				<xsl:when test="//d:title">
					<xsl:value-of select="//d:title"/><xsl:text> </xsl:text>
				</xsl:when>
				<xsl:otherwise>
					<xsl:text>please define title in your docbook file!</xsl:text>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>

		<xsl:choose>
			<xsl:when test="$sequence='blank'">
				<xsl:choose>
					<xsl:when test="$position='center'">
						<xsl:value-of select="$Version"/>
					</xsl:when>

					<xsl:otherwise>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:when>

			<xsl:when test="$pageclass='titlepage'">
			</xsl:when>

			<xsl:when test="$position='center'">
				<xsl:value-of select="$Version"/>
			</xsl:when>

			<xsl:otherwise>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<!-- FOOTER-->
	<xsl:attribute-set name="footer.content.properties">
		<xsl:attribute name="font-family">
			<xsl:value-of select="$body.font.family"/>
		</xsl:attribute>
		<xsl:attribute name="font-size">8pt</xsl:attribute>
	</xsl:attribute-set>

	<xsl:template name="footer.content">
		<xsl:param name="pageclass" select="''"/>
		<xsl:param name="sequence" select="''"/>
		<xsl:param name="position" select="''"/>
		<xsl:param name="gentext-key" select="''"/>

		<xsl:variable name="Version">
			<xsl:choose>
				<xsl:when test="//d:releaseinfo">
					<xsl:value-of select="//d:releaseinfo"/>
				</xsl:when>
				<xsl:otherwise>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>

		<xsl:variable name="Title">
			<xsl:choose>
				<xsl:when test="//d:productname">
					<xsl:value-of select="//d:productname"/><xsl:text> </xsl:text>
				</xsl:when>
				<xsl:otherwise>
					<xsl:text>please define title in your docbook file!</xsl:text>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>

		<xsl:choose>
			<xsl:when test="$sequence='blank'">
				<xsl:choose>
					<xsl:when test="$double.sided != 0 and $position = 'left'">
						<xsl:value-of select="$Version"/>
					</xsl:when>

					<xsl:when test="$double.sided = 0 and $position = 'center'">
					</xsl:when>

					<xsl:otherwise>
						<fo:page-number/>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:when>

			<xsl:when test="$pageclass='titlepage'">
			</xsl:when>

			<xsl:when test="$double.sided != 0 and $sequence = 'even' and $position='left'">
				<fo:page-number/>
			</xsl:when>

			<xsl:when test="$double.sided != 0 and $sequence = 'odd' and $position='right'">
				<fo:page-number/>
			</xsl:when>

			<xsl:when test="$double.sided = 0 and $position='right'">
				<fo:page-number/>
			</xsl:when>

			<xsl:when test="$double.sided != 0 and $sequence = 'odd' and $position='left'">
				<xsl:value-of select="$Version"/>
			</xsl:when>

			<xsl:when test="$double.sided != 0 and $sequence = 'even' and $position='right'">
				<xsl:value-of select="$Version"/>
			</xsl:when>

			<xsl:when test="$double.sided = 0 and $position='left'">
				<xsl:value-of select="$Version"/>
			</xsl:when>

			<xsl:when test="$position='center'">
				<xsl:value-of select="$Title"/>
			</xsl:when>

			<xsl:otherwise>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<xsl:template match="processing-instruction('hard-pagebreak')">
		<fo:block break-before='page'/>
	</xsl:template>


	<!-- PAPER & PAGE SIZE -->

	<!-- Paper type, no headers on blank pages, no double sided printing -->
	<xsl:param name="double.sided">0</xsl:param>
	<xsl:param name="headers.on.blank.pages">0</xsl:param>
	<xsl:param name="footers.on.blank.pages">0</xsl:param>

	<!-- FONTS & STYLES -->

	<xsl:param name="hyphenate">false</xsl:param>

	<!-- Default Font size -->
	<xsl:param name="body.font.family">Helvetica</xsl:param>
	<xsl:param name="body.font.master">10</xsl:param>
	<xsl:param name="body.font.small">8</xsl:param>
	<xsl:param name="title.font.family">Helvetica</xsl:param>

	<!-- Line height in body text -->
	<xsl:param name="line-height">1.4</xsl:param>

	<!-- Chapter title size -->
	<xsl:attribute-set name="chapter.titlepage.recto.style">
		<xsl:attribute name="text-align">left</xsl:attribute>
		<xsl:attribute name="font-weight">bold</xsl:attribute>
		<xsl:attribute name="font-size">
			<xsl:value-of select="$body.font.master * 1.8"/>
			<xsl:text>pt</xsl:text>
		</xsl:attribute>
	</xsl:attribute-set>

	<!-- Why is the font-size for chapters hardcoded in the XSL FO templates?
	Let's remove it, so this sucker can use our attribute-set only... -->
	<xsl:template match="d:title" mode="chapter.titlepage.recto.auto.mode">
		<fo:block xmlns:fo="http://www.w3.org/1999/XSL/Format"
				xsl:use-attribute-sets="chapter.titlepage.recto.style">
			<xsl:call-template name="component.title">
			<xsl:with-param name="node" select="ancestor-or-self::chapter[1]"/>
			</xsl:call-template>
		</fo:block>
	</xsl:template>

	<!-- Sections 1, 2 and 3 titles have a small bump factor and padding -->
	<xsl:attribute-set name="section.title.level1.properties">
		<xsl:attribute name="space-before.optimum">0.6em</xsl:attribute>
		<xsl:attribute name="space-before.minimum">0.6em</xsl:attribute>
		<xsl:attribute name="space-before.maximum">0.6em</xsl:attribute>
		<xsl:attribute name="font-size">
			<xsl:value-of select="$body.font.master * 1.5"/>
			<xsl:text>pt</xsl:text>
		</xsl:attribute>
		<xsl:attribute name="space-after.optimum">0.1em</xsl:attribute>
		<xsl:attribute name="space-after.minimum">0.1em</xsl:attribute>
		<xsl:attribute name="space-after.maximum">0.1em</xsl:attribute>
	</xsl:attribute-set>

	<xsl:attribute-set name="section.title.level2.properties">
		<xsl:attribute name="space-before.optimum">0.4em</xsl:attribute>
		<xsl:attribute name="space-before.minimum">0.4em</xsl:attribute>
		<xsl:attribute name="space-before.maximum">0.4em</xsl:attribute>
		<xsl:attribute name="font-size">
			<xsl:value-of select="$body.font.master * 1.25"/>
			<xsl:text>pt</xsl:text>
		</xsl:attribute>
		<xsl:attribute name="space-after.optimum">0.1em</xsl:attribute>
		<xsl:attribute name="space-after.minimum">0.1em</xsl:attribute>
		<xsl:attribute name="space-after.maximum">0.1em</xsl:attribute>
	</xsl:attribute-set>

	<xsl:attribute-set name="section.title.level3.properties">
		<xsl:attribute name="space-before.optimum">0.4em</xsl:attribute>
		<xsl:attribute name="space-before.minimum">0.4em</xsl:attribute>
		<xsl:attribute name="space-before.maximum">0.4em</xsl:attribute>
		<xsl:attribute name="font-size">
			<xsl:value-of select="$body.font.master * 1.0"/>
			<xsl:text>pt</xsl:text>
		</xsl:attribute>
		<xsl:attribute name="space-after.optimum">0.1em</xsl:attribute>
		<xsl:attribute name="space-after.minimum">0.1em</xsl:attribute>
		<xsl:attribute name="space-after.maximum">0.1em</xsl:attribute>
	</xsl:attribute-set>

	<xsl:attribute-set name="section.title.level4.properties">
		<xsl:attribute name="space-before.optimum">0.3em</xsl:attribute>
		<xsl:attribute name="space-before.minimum">0.3em</xsl:attribute>
		<xsl:attribute name="space-before.maximum">0.3em</xsl:attribute>
		<xsl:attribute name="font-size">
			<xsl:value-of select="$body.font.master * 0.9"/>
			<xsl:text>pt</xsl:text>
		</xsl:attribute>
		<xsl:attribute name="space-after.optimum">0.1em</xsl:attribute>
		<xsl:attribute name="space-after.minimum">0.1em</xsl:attribute>
		<xsl:attribute name="space-after.maximum">0.1em</xsl:attribute>
	</xsl:attribute-set>


	<!-- TABLES -->

	<!-- Some padding inside tables -->
	<xsl:attribute-set name="table.cell.padding">
		<xsl:attribute name="padding-left">4pt</xsl:attribute>
		<xsl:attribute name="padding-right">4pt</xsl:attribute>
		<xsl:attribute name="padding-top">4pt</xsl:attribute>
		<xsl:attribute name="padding-bottom">4pt</xsl:attribute>
	</xsl:attribute-set>

	<!-- Only hairlines as frame and cell borders in tables -->
	<xsl:param name="table.frame.border.thickness">0.1pt</xsl:param>
	<xsl:param name="table.cell.border.thickness">0.1pt</xsl:param>

	<!-- LABELS -->

	<!-- Label Chapters and Sections (numbering) -->
	<xsl:param name="chapter.autolabel" select="1"/>
	<xsl:param name="section.autolabel" select="1"/>
	<xsl:param name="section.autolabel.max.depth" select="1"/>

	<xsl:param name="section.label.includes.component.label" select="1"/>
	<xsl:param name="table.footnote.number.format" select="'1'"/>

	<!-- PROGRAMLISTINGS -->

	<!-- Verbatim text formatting (programlistings) -->
	<xsl:attribute-set name="monospace.verbatim.properties">
		<xsl:attribute name="font-size">7pt</xsl:attribute>
		<xsl:attribute name="wrap-option">wrap</xsl:attribute>
		<xsl:attribute name="keep-together.within-column">1</xsl:attribute>
	</xsl:attribute-set>

	<xsl:attribute-set name="verbatim.properties">
		<xsl:attribute name="space-before.minimum">1em</xsl:attribute>
		<xsl:attribute name="space-before.optimum">1em</xsl:attribute>
		<xsl:attribute name="space-before.maximum">1em</xsl:attribute>
		<xsl:attribute name="space-after.minimum">0.1em</xsl:attribute>
		<xsl:attribute name="space-after.optimum">0.1em</xsl:attribute>
		<xsl:attribute name="space-after.maximum">0.1em</xsl:attribute>

		<xsl:attribute name="border-color">#444444</xsl:attribute>
		<xsl:attribute name="border-style">solid</xsl:attribute>
		<xsl:attribute name="border-width">0.1pt</xsl:attribute>
		<xsl:attribute name="padding-top">0.5em</xsl:attribute>
		<xsl:attribute name="padding-left">0.5em</xsl:attribute>
		<xsl:attribute name="padding-right">0.5em</xsl:attribute>
		<xsl:attribute name="padding-bottom">0.5em</xsl:attribute>
		<xsl:attribute name="margin-left">0.5em</xsl:attribute>
		<xsl:attribute name="margin-right">0.5em</xsl:attribute>
	</xsl:attribute-set>

	<!-- Shade (background) programlistings -->
	<xsl:param name="shade.verbatim">1</xsl:param>
	<xsl:attribute-set name="shade.verbatim.style">
		<xsl:attribute name="background-color">#F0F0F0</xsl:attribute>
	</xsl:attribute-set>

	<xsl:attribute-set name="list.block.spacing">
		<xsl:attribute name="space-before.optimum">0.1em</xsl:attribute>
		<xsl:attribute name="space-before.minimum">0.1em</xsl:attribute>
		<xsl:attribute name="space-before.maximum">0.1em</xsl:attribute>
		<xsl:attribute name="space-after.optimum">0.1em</xsl:attribute>
		<xsl:attribute name="space-after.minimum">0.1em</xsl:attribute>
		<xsl:attribute name="space-after.maximum">0.1em</xsl:attribute>
	</xsl:attribute-set>

	<xsl:attribute-set name="example.properties">
		<xsl:attribute name="space-before.minimum">0.5em</xsl:attribute>
		<xsl:attribute name="space-before.optimum">0.5em</xsl:attribute>
		<xsl:attribute name="space-before.maximum">0.5em</xsl:attribute>
		<xsl:attribute name="space-after.minimum">0.1em</xsl:attribute>
		<xsl:attribute name="space-after.optimum">0.1em</xsl:attribute>
		<xsl:attribute name="space-after.maximum">0.1em</xsl:attribute>
	</xsl:attribute-set>

	<xsl:attribute-set name="sidebar.properties">
		<xsl:attribute name="border-color">#444444</xsl:attribute>
		<xsl:attribute name="border-style">solid</xsl:attribute>
		<xsl:attribute name="border-width">0.1pt</xsl:attribute>
		<xsl:attribute name="background-color">#F0F0F0</xsl:attribute>
	</xsl:attribute-set>


	<!-- TITLE INFORMATION FOR FIGURES, EXAMPLES ETC. -->

	<xsl:attribute-set name="formal.title.properties" use-attribute-sets="normal.para.spacing">
		<xsl:attribute name="font-weight">normal</xsl:attribute>
		<xsl:attribute name="font-style">italic</xsl:attribute>
		<xsl:attribute name="font-size">
			<xsl:value-of select="$body.font.master"/>
			<xsl:text>pt</xsl:text>
		</xsl:attribute>
		<xsl:attribute name="hyphenate">false</xsl:attribute>
		<xsl:attribute name="space-before.minimum">0.1em</xsl:attribute>
		<xsl:attribute name="space-before.optimum">0.1em</xsl:attribute>
		<xsl:attribute name="space-before.maximum">0.1em</xsl:attribute>
	</xsl:attribute-set>

	<!-- CALLOUTS -->

	<!-- don't use images for callouts -->
	<xsl:param name="callout.graphics">0</xsl:param>
	<xsl:param name="callout.unicode">1</xsl:param>

	<!-- Place callout marks at this column in annotated areas -->
	<xsl:param name="callout.defaultcolumn">90</xsl:param>

	<!-- MISC -->

	<!-- Placement of titles -->
	<xsl:param name="formal.title.placement">
		figure after
		example after
		equation before
		table before
		procedure before
	</xsl:param>

	<!-- Format Variable Lists as Blocks (prevents horizontal overflow) -->
	<xsl:param name="variablelist.as.blocks">1</xsl:param>
	<xsl:param name="body.start.indent">0pt</xsl:param>

	<!-- Remove "Chapter" from the Chapter titles... -->
	<xsl:param name="local.l10n.xml" select="document('')"/>
	<l:i18n xmlns:l="http://docbook.sourceforge.net/xmlns/l10n/1.0">
		<l:l10n language="en">
			<l:context name="title-numbered">
				<l:template name="chapter" text="%n.&#160;%t"/>
				<l:template name="section" text="%n&#160;%t"/>
			</l:context>
			<l:context name="title">
				<l:template name="example" text="Example&#160;%n&#160;%t"/>
			</l:context>
		</l:l10n>
	</l:i18n>

	<!-- admon -->
	<xsl:param name="admon.graphics" select="0"/>

	<xsl:attribute-set name="nongraphical.admonition.properties">
		<xsl:attribute name="margin-left">0.1em</xsl:attribute>
		<xsl:attribute name="margin-right">2em</xsl:attribute>
		<xsl:attribute name="border-left-width">.75pt</xsl:attribute>
		<xsl:attribute name="border-left-style">solid</xsl:attribute>
		<xsl:attribute name="border-left-color">#5c5c4f</xsl:attribute>
		<xsl:attribute name="padding-left">0.5em</xsl:attribute>
		<xsl:attribute name="space-before.optimum">1.5em</xsl:attribute>
		<xsl:attribute name="space-before.minimum">1.5em</xsl:attribute>
		<xsl:attribute name="space-before.maximum">1.5em</xsl:attribute>
		<xsl:attribute name="space-after.optimum">1.5em</xsl:attribute>
		<xsl:attribute name="space-after.minimum">1.5em</xsl:attribute>
		<xsl:attribute name="space-after.maximum">1.5em</xsl:attribute>
	</xsl:attribute-set>

    <xsl:attribute-set name="admonition.title.properties">
		<xsl:attribute name="font-size">10pt</xsl:attribute>
		<xsl:attribute name="font-weight">bold</xsl:attribute>
		<xsl:attribute name="hyphenate">false</xsl:attribute>
		<xsl:attribute name="keep-with-next.within-column">always</xsl:attribute>
		<xsl:attribute name="margin-left">0</xsl:attribute>
	</xsl:attribute-set>

	<xsl:attribute-set name="admonition.properties">
		<xsl:attribute name="space-before.optimum">0em</xsl:attribute>
		<xsl:attribute name="space-before.minimum">0em</xsl:attribute>
		<xsl:attribute name="space-before.maximum">0em</xsl:attribute>
	</xsl:attribute-set>

	<!-- Asciidoc -->
	<xsl:template match="processing-instruction('asciidoc-br')">
		<fo:block/>
	</xsl:template>

	<xsl:template match="processing-instruction('asciidoc-hr')">
		<fo:block space-after="1em">
			<fo:leader leader-pattern="rule" rule-thickness="0.5pt" rule-style="solid" leader-length.minimum="100%"/>
		</fo:block>
	</xsl:template>

  <xsl:template match="processing-instruction('asciidoc-pagebreak')">
    <fo:block break-after='page'/>
  </xsl:template>

	<!-- SYNTAX HIGHLIGHT -->

	<xsl:template match='xslthl:keyword' mode="xslthl">
	  <fo:inline font-weight="bold" color="#7F0055"><xsl:apply-templates mode="xslthl"/></fo:inline>
	</xsl:template>

	<xsl:template match='xslthl:string' mode="xslthl">
	  <fo:inline font-weight="bold" font-style="italic" color="#2A00FF"><xsl:apply-templates mode="xslthl"/></fo:inline>
	</xsl:template>

	<xsl:template match='xslthl:comment' mode="xslthl">
	  <fo:inline font-style="italic" color="#3F5FBF"><xsl:apply-templates mode="xslthl"/></fo:inline>
	</xsl:template>

	<xsl:template match='xslthl:tag' mode="xslthl">
	  <fo:inline font-weight="bold" color="#3F7F7F"><xsl:apply-templates mode="xslthl"/></fo:inline>
	</xsl:template>

	<xsl:template match='xslthl:attribute' mode="xslthl">
	  <fo:inline font-weight="bold" color="#7F007F"><xsl:apply-templates mode="xslthl"/></fo:inline>
	</xsl:template>

	<xsl:template match='xslthl:value' mode="xslthl">
	  <fo:inline font-weight="bold" color="#2A00FF"><xsl:apply-templates mode="xslthl"/></fo:inline>
	</xsl:template>

</xsl:stylesheet>
