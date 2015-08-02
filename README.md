# eBayBidSearchService


Search item service:

   provided item id, if the item exists, the service will return all the relevant infomation about the item
    
    
keyword search service:

   Given a keyword such as "green",  the service will return all items which has word "green" in its name or in its description


Build steps:

● use Java xml parser to parse ebay bidding history

● desgin databse schema for storing extracted useful inforamtion

● use Apache Lucene to index bidding inforamtion 

● implement two search functions: keyword search and item search

● publish search function as RESTful web service using JAX-RS and Jersey
