# Introduction

This README lives at https://github.com/springframework/gh-pages#readme.
If you're seeing it within a specific Spring project's 'gh-pages' branch, that's
because this file, along with other files gets periodically merged in order to
share common content like headers and footers, CSS, etc.

If you're reading this file 

## How to start a new `gh-pages` project page
From within your Spring project's git checkout directory:

    git remote add gh-pages-upstream https://github.com/springframework/gh-pages.git
    git checkout --orphan gh-pages
    git pull gh-pages-upstream gh-pages


## Create index.html
1. Copy common-index.html to index.html
2. Update 

## Commit your changes
    git push origin gh-pages


# How to keep common gh-pages content up to date
    git checkout gh-pages
    git pull --no-ff gh-pages-upstream gh-pages
