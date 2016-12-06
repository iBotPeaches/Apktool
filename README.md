### Apktool Documentation
**This is the repository for the [https://ibotpeaches.github.io/Apktool](https://ibotpeaches.github.io/Apktool) site. If you are looking for the Apktool tool repository. Click [here](https://github.com/iBotPeaches/Apktool).**

This repository contains the files used in generating the Github Pages site. It's possible to test them locally so you can test changes prior to making pull requests.


## How to run locally
1. Install [Ruby](https://www.ruby-lang.org/en/documentation/installation/)
2. Install Bundler `gem install bundler`
3. Clone repo `git clone git@github.com:iBotPeaches/Apktool.git`
4. Checkout wiki `git checkout gh-pages`
5. Install dependencies `bundle install`
6. Spin up local server `bundle exec jekyll serve`

## Known Issues

**Files in `_includes` have to be written with raw HTML**  
 - Not sure how to execute markdown via an include command. Doesn't work with versions of dependencies installed on Github pages
 
**Dates in `_data/releases.yml` have to include the timestamp like so `2010-03-02 00:00:0`**
 - I believe this is a YAML limitation, so make sure to always have a `00:00:0` timestamp added to days
 
**Links in `_data/releases.yml` have to be hardcoded to live site**
 - appending `/Apktool/` because `{{ site.github.url }}` doesn't work