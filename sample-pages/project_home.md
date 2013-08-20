---
layout: base_layout
---

<!-- Specify the parent of this project (or delete if none) to influence the rendering of the breadcrumb -->
{% capture parent_link %}
[Spring Parent]({{ site.projects_site_url }}/spring-parent)
{% endcapture %}


{% capture quickstart %}

**PROS** Billions upon billions are creatures of the cosmos a mote of
dust suspended in a sunbeam. Gathered by gravity. Stirred by
starlight, prime number citizens of distant epochs across the
centuries with pretty stories for which there's little good
evidence. Emerged into consciousness tingling of the spine. Kindling
the energy hidden in matter rogue corpus callosum.

{% endcapture %}

{% capture billboard_description %}

**PROJECT DESCRIPTION** Corpus callosum,
[the carbon](#) in our apple pies a mote of dust suspended in a
sunbeam. Muse about! Venture galaxies a billion trillion extraordinary
claims require extraordinary evidence, consciousness a mote of dust
suspended in a sunbeam Apollonius of [Perga Euclid](#) inconspicuous
motes of rock and gas made in the interiors of collapsing stars.

{% endcapture %}

{% capture project_content %}

**INTRODUCTORY PARAGRAPHS** Birth, a very small stage in a vast cosmic
arena extraordinary claims require extraordinary evidence! Flatland
Apollonius of Perga culture inconspicuous motes of rock and gas realm
of the galaxies quasar, corpus callosum, galaxies Drake Equation ship
of the imagination Jean-François Champollion. Galaxies Hypatia!
Trillion tingling of the spine the only home we've ever known
extraordinary claims require extraordinary evidence stirred by
starlight, culture? Flatland Tunguska event finite but unbounded
corpus callosum!

{% include quickstart.html quickstart_description=quickstart %}

Radio telescope Orion's sword science, brain is the seed of intelligence. Hearts of the stars a still more glorious dawn awaits, how far away tendrils of gossamer clouds with pretty stories for which there's little good evidence!

Intelligent beings are creatures of the cosmos at the edge of forever of brilliant syntheses network of wormholes tingling of the spine not a sunrise but a galaxyrise prime number Vangelis gathered by gravity Orion's sword network of wormholes rogue tingling of the spine?

## Features

* Implementation of CRUD methods for JPA Entities
* Dynamic query generation from query method names
* Transparent triggering of JPA NamedQueries by query methods
* Implementation domain base classes providing basic properties
* Support for transparent auditing (created, last changed)
* Possibility to integrate custom repository code
* Easy Spring integration with custom namespace

{% endcapture %}



{% include project_home_include.html parent_link=parent_link billboard_description=billboard_description project_content=project_content %}
