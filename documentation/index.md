---
layout: other
title: Apktool - Documentation
description: Apktool - Documentation (Decoding, Rebuilding, FrameworkFiles, 9patch images)
---

<div class="row">
  <div class="col-md-9" data-target="#affixNav">
    {% for category in site.data.categories %}
      <h2 id="{{ category.title | slugify }}">{{ category.title }}</h2>
      <hr />
      {% for sub in category.subs %}
        <h3 id="{{ sub | slugify }}">{{ sub }}</h3>
        <hr />
        <p>
          {% include {{ category.title | slugify }}-{{ sub | slugify }}.md %}
        </p>
      {% endfor %}
    {% endfor %}
  </div>
  <div class="col-md-3">
    <nav class="hidden-sm hidden-xs affix" id="affixNav">
      <ul class="nav sidenav" data-spy="affix" data-offset-top="10">
        {% for category in site.data.categories %}
          <li>
            <a href="#{{ category.title | slugify }}">{{ category.title }}</a>
            <ul class="nav">
              {% for sub in category.subs %}
                <li><a href="#{{ sub | slugify }}">{{ sub }}</a></li>
              {% endfor %}
            </ul>
          </li>
        {% endfor %}
      </ul>
    </nav>
  </div>
</div>