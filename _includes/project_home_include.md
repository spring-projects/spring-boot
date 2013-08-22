{% include billboard.md %}

<div class="billboard-body--wrapper project-body--container" id="quick-start">
<div class="github-fork-ribbon--wrapper">
<div class="github-fork-ribbon">
<a href="{{ site.github_repo_url }}">
<i class="icon icon-github"></i>
Fork me on GitHub
</a>
</div>
</div>
<div class="row-fluid">
<div class="span8">
<div class="project-body--section">
{{ include.main_content | markdownify }}
</div>
</div>
<div class="span4">{% include project_sidebar.md %}</div>
</div>
</div>
