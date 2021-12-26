# TODO


## both

- switch toggled home page.
  - more visuals and images.
- music background tutorial video/gif.
- launch checklist.
  - reddit & blog post.
  print flyers in gym & downtown streets.
- Create Instagram, reddit and facebook account.

## wa

- newsletter welcome and update email.
- entity search functionality.
- buymeacoffee
- cache clear on deploy.

## dc

- twitter bot for daily post. (WIP)
- entity name cleanup.
  - E.g. Merkel's
- collect feeds that did not work at each cycle.
  - may need automated admin notification feature.
- title cleanup.
  - google news title contains news outlet name after "-"
- description cleanup.
  - in general, the sentence dl gets unclean text with 
    urls and html escape tags during data prep.
  - Fix description fetching for image captions.
    - recursive is going into nested items.
    - remove zeroedge. long description with image captions.
    - e.g. remove slate content. unrelated image descriptions.
    - remove axios content too long. has content:encoded for html
      formatted content that can be parsed.
  - meaningful html escape chars and tags in text.
    - e.g. e denials,&lt;strong&gt; &lt;/strong&gt;according 
      - does system see html tags to explore or escape chars?
        - also seen in reddit & axios content.
    - could use content:encoded when available.
  - google news nested headlines
    - also contains news outlet name in neighbor html tag.
    - last list item in description is default message to go to google.
- automation:
  - rss feed URL finder script using nav bar links.

## non-urgent

- call cache warm after update.
- scrape entrypoint router for xml vs REST scrape.
- Hardcoded books links for each category.
- look into @samanthasunne and toolsforreporters website.
- add admin dashboard.
- paginated analysis page view.
- Add news API to scraper. (not a lot of data, needs scraper router).
- Precomputing texts for top x topics in each category and add to cache.
- add context column.
- table column names to shared.
- dynamic entity book url scraper.
- add roadmap section. e.g. filtering feeds.
- move to bootstrap 5.1
- enable pagination for text and entity pages.
- add historical data to s3/r2 for API.
- make footer look professional (bg color, sitemap, copyright, policy).
