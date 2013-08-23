# Introduction

Spring's project pages at http://projects.springframework.io are based on Jekyll and gh-pages.
In order to keep everything looking similar, common elements of the Jekyll site layout, css, etc
are stored in this shared gh-pages repository. If you're seeing this README in the gh-pages branch
of an actual Spring project, that's because this file, along with all the other common files
get merged periodically into each project.

It sounds a little funky (and it is), but it's way better than the misery of Git submodules.
It's actually pretty easy. If you're just getting started, the follow the directions immediately
below. If you're needing a refresher on how to keep things up to date, then head to the section
at the bottom.

## How to start a new `gh-pages` project page
From within your Spring project's git checkout directory:

    git remote add gh-pages-upstream https://github.com/spring-projects/gh-pages.git
    git checkout --orphan gh-pages
    git pull gh-pages-upstream gh-pages

## Create index.html

1. Copy common-index.html to index.html
2. Update the information within, build out the content of your site

## View the site locally

Follow the instructions at http://jekyllrb.com, which are really just as simple as the following
(assuming your project name is "spring-xyz"):

    gem install jekyll
    jekyll new spring-xyz
    cd spring-xyz
    jekyll serve --watch

## Commit your changes

    git push origin gh-pages


# How to keep common gh-pages content up to date

Once you've set up your project's gh-pages infrastructure above, you'll get notified whenever
changes are made to the shared gh-pages project. Actually, _your project_ will get notified,
because a webhook configured on the gh-pages project will fire that ultimately creates a new
issue in your project's GH Issues.
> Yeah, it would have been nice to do proper pull requests instead of issues, but this arrangement
with gh-pages branches and upstreams isn't based on forking. That means pull requests are no can do.
This also means that if your project doesn't have issues turned on, you won't get notified! So you
might consider doing that if you haven't already.

In any case, whether or not you get notified via the issue coming in from the webhook, sync'ing
up to the latest from the shared gh-pages project is easy enough:

> This assumes you've got the `gh-pages-upstream` remote set up, per the instructions in sections
above.

    git checkout gh-pages
    git pull --no-ff gh-pages-upstream gh-pages
