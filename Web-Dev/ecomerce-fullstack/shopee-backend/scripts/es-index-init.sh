#!/bin/bash
# ============================================================
#  Elasticsearch — Create products index with optimal settings
#  Run once on new cluster setup.
# ============================================================
set -e

ES_URL="${ELASTICSEARCH_URI:-http://localhost:9200}"

echo "Creating products index mapping on $ES_URL..."

curl -X PUT "$ES_URL/products" \
  -H "Content-Type: application/json" \
  -d '{
    "settings": {
      "number_of_shards": 3,
      "number_of_replicas": 1,
      "analysis": {
        "analyzer": {
          "product_analyzer": {
            "type": "custom",
            "tokenizer": "standard",
            "filter": ["lowercase", "asciifolding", "stop"]
          }
        }
      }
    },
    "mappings": {
      "properties": {
        "name":             { "type": "text",    "analyzer": "product_analyzer", "boost": 3.0 },
        "shortDescription": { "type": "text",    "analyzer": "product_analyzer", "boost": 2.0 },
        "description":      { "type": "text",    "analyzer": "product_analyzer" },
        "slug":             { "type": "keyword" },
        "basePrice":        { "type": "double" },
        "originalPrice":    { "type": "double" },
        "categoryId":       { "type": "keyword" },
        "categorySlug":     { "type": "keyword" },
        "categoryName":     { "type": "keyword" },
        "tags":             { "type": "keyword" },
        "status":           { "type": "keyword" },
        "featured":         { "type": "boolean" },
        "ratingAvg":        { "type": "float"   },
        "ratingCount":      { "type": "integer" },
        "soldCount":        { "type": "long"    },
        "totalStock":       { "type": "integer" },
        "createdAt":        { "type": "date"    },
        "updatedAt":        { "type": "date"    }
      }
    }
  }' | python3 -m json.tool

echo ""
echo "✓ Products index ready."
