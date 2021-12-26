# TODO

## both

- table column names to shared.

## wa
- add context column.
- PG read apis.
  - connection pooling.
  - redo page data fetcher.
- slimmer docker image and dockerfile.

## dc

- call cache flush & warm after update.
- Add news API to scraper.
- Hardcoded books links for each category.
- description exceptions:
  - remove zeroedge. invalid text skil description load on zerohedge.com.
  - mfool description has too many reporter names.
  - feed url class that has descriptionClean flag.
- Precomputing texts for top x topics in each category and add to cache.
- Percentile total count filter. 
- site total count vs. neg count, 
- remove neg IDs from pos ids.

## non-urgent

- dynamic entity book url scraper.
- YouTube tutorial video.
- add roadmap section. e.g. filtering feeds.
- move to bootstrap 5.1
- identify empty DF and slow read cache bug and write UT.
- enable pagination for text and entity pages.
- add historical data to s3/r2 for API.
- make footer look professional (bg color, sitemap, copyright, policy).
