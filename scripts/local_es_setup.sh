curl -XPUT localhost:9200/_template/template_1 -d '
{
    "template" : "foxtrot-*",
    "settings" : {
        "number_of_shards" : 10,
        "number_of_replicas" : 0
    },
    "mappings" : {
        "document" : {
            "_source" : { "enabled" : false },
            "_all" : { "enabled" : false },
            "_timestamp" : { "enabled" : true },

            "dynamic_templates" : [
	            {
	                "template_timestamp" : {
	                    "match" : "timestamp",
	                    "mapping" : {
	                        "store" : false,
	                        "index" : "not_analyzed",
	                        "type" : "date"
	                    }
	                }
                },
                {
	                "template_no_store" : {
	                    "match" : "*",
	                    "mapping" : {
	                        "store" : false,
	                        "index" : "not_analyzed"
	                    }
	                }
	            }
            ]
        }
    }
}'

curl -XPUT 'http://localhost:9200/consoles/' -d '{
    "settings" : {
        "index" : {
            "number_of_shards" : 1,
            "number_of_replicas" : 0
        }
    }
}'

curl -XPUT 'http://localhost:9200/table-meta/' -d '{
    "settings" : {
        "index" : {
            "number_of_shards" : 1,
            "number_of_replicas" : 0
        }
    }
}'