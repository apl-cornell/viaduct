# Updating Documentation

We use MkDocs to generate documentation. See:

* [mkdocs documentation](http://mkdocs.org).
* [mkdocs-material documentation](https://squidfunk.github.io/mkdocs-material/).

## Project layout

    docs/
        mkdocs.yml    # The configuration file.
        src/
            index.md  # The documentation homepage.
            ...       # Other markdown pages, images and other files.

## Gradle tasks

* `mkdocsBuild` - Build the documentation site.
* `mkdocsServe --no-daemon` - Start the live-reloading docs server.
