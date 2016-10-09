# Introduction

Spring's project pages are based on [Jekyll](http://jekyllrb.com) and [GitHub Pages](http://pages.github.com/). This means that each project's website is stored within the project repo, in a special branch called "gh-pages". In order to keep everything looking similar across the many individual Spring projects, common elements of the Jekyll site layout, css, etc are stored in [a shared gh-pages repository](http://github.com/spring-projects/gh-pages). If you're seeing this README in the gh-pages branch of an actual Spring project, that's because this file, along with all the other common files get merged periodically into each project.

This approach may sound a little funky (and it is), but it's way better than the misery of Git submodules. In fact, it's actually pretty easy. If you're just getting started, then follow the directions immediately below. If you're needing a refresher on how to keep things up to date, then head to the section at the bottom on "keeping up to date".

> ***Note:*** Github changed their rendering (a lot) in April 2016. If you are seeing pages build failures since that time, then you probably need to merge some changes from this upstream. Follow the instructions below, and for testing locally use rubiy 2.x and don't forget to `bundle exec ...` everything.



# How to start a new `gh-pages` project page

From within your Spring project's git checkout directory:

### Create and checkout the gh-pages branch

    git checkout --orphan gh-pages

### Remove all files

    git rm -rf `git ls-files` && rm -rf *

### Add the gh-pages-upstream remote

    git remote add gh-pages-upstream https://github.com/spring-projects/gh-pages.git

### Pull in the common site infrastructure

    git pull gh-pages-upstream gh-pages


## Edit `_config.yml`

You'll need to tweak a few settings in `_config.yml`, sitting in the root directory. Edit this file and follow the instructions within. It should be self explanatory; you'll see that there are lots of instances of "spring-framework" as a project name. You should replace these with your own project's information as appropriate.


## Create the project home page

### What kind of project are you?

At this point, you just need to give your project a home page. There are pre-fab samples included in the `_sample-pages` directory:

    $ ls -1 _sample-pages
    documentation.md
    project_group.html
    project.html
    vaniall.md

At this point you need to make a decision: is your project a "grouping project", like Spring Data, acting as a parent for one or more child projects? If so, you should choose `project_group.html` in the following step. Otherwise, if you're dealing with an individual, concrete Spring project, e.g. Spring Data JPA, or Spring Security OAuth, you should choose `project.html`. The `documentation.md` can be used when you want to expose README.md files on your site.

### Copy the sample page to `index.html`

Based on your choice above, copy the correct sample page to `index.html`, e.g.:

    $ cp _sample-pages/project.html index.html

or

    $ cp _sample-pages/project_group.html index.html


## Edit the content of your home page

### Edit the YAML front matter

Open up `index.html` in your favorite text editor. Within, you'll find "YAML front matter" at the top of the file, i.e. everything between the triple dashes that look like `---`. This section contains some basic metadata, and—if you've copied the `project.html` sample—also contains a custom section of information for your project "badges". These are icons that will render in the sidebar of your project page.

Edit the URLs and other values in the YAML front matter as appropriate. This should be self-explanatory.

> **Tip**: If a particular "badge" (e.g. Sonar) does not apply to your project, just delete that entry from the YAML.

### Understand the use of "captures" and "includes"

Next, you'll see a series of `{% capture variable_name %}` blocks that look like this:

    {% capture billboard_description %}
    ...
    {% endcapture %}

At the end of the file you'll see a `{% include %}` directive that looks like this:

    {% include project_page.html %}

That `include` directive is the most important part. It brings in `project_page.html`, which will actually be responsible for rendering the page content.

The net effect is that you have one simple place to edit actual page content, and the actual rendering and placement of that content is handled for you by the include.

Ideally, you shouldn't have to touch any other files besides `_config.yml` and `index.html`, but of course if you need to you always can.

### Replace the filler text with copy specific to your project

Of course the next step is simply to change the prose within each `{% capture %}` block to read as you want it to. It's just Markdown, so do as you please with that in mind. Note that you'll want to move on to the next step to view the site locally before you get too far into the editing process; this will allow you view your changes live as you make them.


## View the site locally

Assuming you're already within your project's clone directory, and you've already checked out the `gh-pages` branch, follow these simple directions to view your site locally:

### Install jekyll if you have not already

> **Note:** Jekyll 3.0.4 is a known good version, and it is specifically referred to in `Gemfile.lock` so you have to use `bundle` (not `gem install ...`) to install it:

    gem install bundler
    bundle

### Run jekyll

Use the `--watch` flag to pick up changes to files as you make them, allowing you a nice edit-and-refresh workflow.

    bundle exec jekyll serve --watch

> **Important:** Because the `baseurl` is set explicitly within your project's `_config.yml` file, you'll need to fully-qualify the URL to view your project. For example, if your project is named "spring-xyz", your URL when running Jekyll locally will be <http://localhost:4000/spring-xyz/>. Don't forget the trailing slash! You'll get a 404 without it.


## Commit your changes

Once you're satisified with your edits, commit your changes and push the `gh-pages` pages up to your project's `origin` remote.

    git commit -m "Initialize project page"
    git push --set-upstream origin gh-pages
    git push


## View your site live on the web

That's it! After not more than a few minutes, you should be able to see your site at http://spring-projects.github.io/{your-spring-project}


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
