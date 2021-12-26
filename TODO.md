# TODO


## wa
- slimmer docker image and dockerfile.
  - test with docker run.

## dc

- call cache flush & warm after update.
- Add news API to scraper.
- Hardcoded books links for each category.
- description exceptions:
  - remove zeroedge. invalid text skill description load on zerohedge.com.
  - mfool description has too many reporter names.
  - feed url class that has descriptionClean flag.
  - set category of feed URL in class.
- Precomputing texts for top x topics in each category and add to cache.
- Percentile total count filter. 
- site total count vs. neg count, 
- remove neg IDs from pos ids.

## non-urgent

- add context column.
- table column names to shared.
- dynamic entity book url scraper.
- YouTube tutorial video.
- add roadmap section. e.g. filtering feeds.
- move to bootstrap 5.1
- identify empty DF and slow read cache bug and write UT.
- enable pagination for text and entity pages.
- add historical data to s3/r2 for API.
- make footer look professional (bg color, sitemap, copyright, policy).
