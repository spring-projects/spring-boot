<div class="billboard--wrapper project-header--wrapper">
<div class="billboard--container">
<div class="container-fluid">
<div  class="content--title">
{% capture breadcrumb %}
[Projects]({{ site.projects_site_url }}){% if include.parent_link %} : {{ include.parent_link }}{% endif %}
{% endcapture %}
{{ breadcrumb | markdownify }}
</div>
<div class="row-fluid">
<div class="span8">
<div class="project--links--container">
<a href="{{ site.github_repo_url }}" class="project-link">
<i class="icon-github"></i>
</a>
<a href="{{ site.forum }}" class="project-link project-link-forum">
<div class="spring-icon spring-icon-forum"></div>
</a>
</div>
<div class="project--title">{{ site.name }}</div>

<div class="project--description">
{{ include.billboard_description | markdownify }}
</div>
<a class="btn btn-black uppercase project-quickstart-btn" href="#quick-start">Quick Start</a>
</div>
</div>
</div>
</div>
<div class="billboard-bg">
<div class="billboard-icon icon-spring-data-large"></div>
</div>
</div>
