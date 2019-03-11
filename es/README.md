## Install, configure and start Elasticsearch

### Manual installation

Download Elasticsearch from https://www.elastic.co/fr/downloads/elasticsearch
and copy it to the ES module. eg. es/elasticsearch-5.0.0

or run "mvn install -Pes-download"

Start ES.

Configure index
```
curl -X PUT http://localhost:9200/features -d @config/features.json
```

### Maven installation

Maven could take care of the installation steps:
* download
* initialize collection
* start

Use the following commands:

```
cd es
mvn install -Pes-download
mvn exec:exec -Des-start
```


Optionnaly you can manually create index but they will be created by the catalogue when 
the Elastic instance is available and if index does not exist.
```
DATADIR=../web/src/main/webapp/WEB-INF/data/config/index/
curl -X PUT http://localhost:9200/gn-records -H "Content-Type:application/json"  -d @$DATADIR/records.json
curl -X PUT http://localhost:9200/gn-features -H "Content-Type:application/json" -d @$DATADIR/features.json
curl -X PUT http://localhost:9200/gn-searchlogs -H "Content-Type:application/json"  -d @$DATADIR/searchlogs.json
```

To delete your index:

```
curl -X DELETE http://localhost:9200/gn-records
curl -X DELETE http://localhost:9200/gn-features
curl -X DELETE http://localhost:9200/gn-searchlogs
```



### Production use

Configure ES to start on server startup.

