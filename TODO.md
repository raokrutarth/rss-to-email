# TODO


## both

- shared config class.

## wa

- generic db layer get function which takes RS convertor function.
- newsletter signup form.
  - redis verify endpoint.
- reddit & blog post.
- pagination.

## dc

- Hardcoded books links for each category.
- scrape entrypoint router for xml vs REST scrape.
- manual exceptions:
  - remove zeroedge. invalid text description.
    - feed url class's descriptionClean flag.
  - set category of feed URL in class.
  - remove mfool descriptions due to reporter name. 
- twitter bot for daily post. (WIP)
- call cache warm after update.

## non-urgent

- Add news API to scraper. (not a lot of data, needs scraper router).
- Precomputing texts for top x topics in each category and add to cache.
- music background video.
- add context column.
- table column names to shared.
- dynamic entity book url scraper.
- YouTube tutorial video.
- add roadmap section. e.g. filtering feeds.
- move to bootstrap 5.1
- enable pagination for text and entity pages.
- add historical data to s3/r2 for API.
- make footer look professional (bg color, sitemap, copyright, policy).
