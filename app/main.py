"""
https://github.com/Smattr/needtoknow
https://ifttt.com/applets/YnbGBZDy
https://ifttt.com/applets/209884p-add-rss-feed-to-daily-e-mail-digest

https://github.com/randombit/grab-rss

"""
import requests
from bs4 import BeautifulSoup
from json import loads
import untangle
import xmltodict

from timeit import timeit

feeds = []
with open("./feeds.json") as f:
    feeds = loads(f.read())


r = requests.get(feeds[0]["url"])
print("The scraping job succeeded: ", r.status_code)


def t1():
    return xmltodict.parse(r.text)


def t2():
    return untangle.parse(r.text)


assert t1() is not None
print(t1())

assert t2() is not None

print(
    timeit("t1()", globals=globals(), number=10),
    timeit("t2()", globals=globals(), number=10),
)

# from pprint import pprint

# pprint(res)
exit()


soup = BeautifulSoup(r.content, features="xml")
articles = soup.findAll("item")

article_list = []
for a in articles:
    print(a)
    title = a.find("title").text
    link = a.find("link").text
    published = a.find("pubDate").text
    article = {"title": title, "link": link, "published": published}
    article_list.append(article)
print(article_list)


if __name__ == "__main__":
    print("Hello")
