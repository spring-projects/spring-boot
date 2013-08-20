{% include billboard.md %}

<div class="billboard-body--wrapper project-body--container">
<div class="row-fluid">
<div class="span8">
<div class="project-body--section">
{{ include.project_content | markdownify }}
</div>
</div>
<div class="span4">{% include project_sidebar.md %}</div>
</div>
</div>