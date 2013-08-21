{% capture cilink %}
<a href="https://drone.io/github.com/springframework-meta/gs-rest-service/latest">
<img src="https://drone.io/github.com/springframework-meta/gs-rest-service/status.png">
</a>
{% endcapture %}

{% capture travislink %}
[![Build Status](https://travis-ci.org/SpringSource/spring-boot.png)](https://travis-ci.org/SpringSource/spring-boot)
{% endcapture %}

{% capture bamboostatus %}
[Bamboo Status](https://build.springsource.org/browse/XD)
{% endcapture %}

{% include badge.md link=cilink %}
{% include badge.md link=travislink %}
{% include badge.md link=bamboostatus %}
