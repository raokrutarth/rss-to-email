# TODO

- deploy to gcp with 2GB ram.
- write book url scraper.
- write talks url scraper.
- add screenshots to public image folder.
  - sourcs.
  - num mentions
  - mentions table.
- make footer look professional.
- add data to prod keyspace for website to read.


## non-urgent

- enable pagination for text and entity pages.
- add historical data to s3/r2 for API.
- find free tier that works with 1GB ram.
  - GCP (too many errors, complex secrets management & poor cli logging)
  - Azure container instances?
- look  into faster load time with enbedded javascript.
  - <https://stackoverflow.com/questions/23906956/show-loading-icon-until-the-page-is-load> 
- move to redis cache for pages 
  - <https://github.com/KarelCemus/play-redis/blob/master/doc/20-configuration.md>