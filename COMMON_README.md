---
---

Projects GH Pages Common Content
================================

## To start a new 'gh-pages' Project page:

	git remote add gh-pages-common https://github.com/springframework/common.git
	git checkout --orphan gh-pages
	git pull gh-pages-common master

Add a _config.yml file and add your project info:

	name: Spring Data JPA
	baseurl: /spring-data-jpa

## To create a new page:

Create a file (e.g. index.html) with this at the top:

	---
	layout: default
	title: Spring Framework - Projects
	---

	YOUR CONTENT HERE


After you've done your custom page:

	git push origin gh-pages


## To update common content:

	git checkout gh-pages
	git pull gh-pages-common master
