# TODO

## both

## wa
- add context column.

## dc

- call cache flush & warm after update.
- Add news API to scraper.
- description exceptions:
  - remove zeroedge. invalid text skil description load on zerohedge.com.
  - mfool description has too many reporter names.
- Precomputing texts for top x topics in each category and add to cache.
- Hardcoded books links for each category.
- Percentile total count filter. 
- site total count vs. neg count, 
- remove neg IDs from pos ids, 
- improve sentiment analysis models.
- feed url class that haas descriptionClean flag.

## non-urgent

- YouTube tutorial video.
- write book url scraper.
- add roadmap section. e.g. filtering feeds.
- move to bootstrap 5.1
- identify empty DF and slow read cache bug and write UT.
- enable pagination for text and entity pages.
- add historical data to s3/r2 for API.
- look into faster load time with enbedded javascript.
  - <https://stackoverflow.com/questions/23906956/show-loading-icon-until-the-page-is-load> 
- move to redis cache for pages 
  - <https://github.com/KarelCemus/play-redis/blob/master/doc/20-configuration.md>
- make footer look professional (bg color, sitemap, copyright, policy).
