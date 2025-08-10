// https://neo4j.com/docs/apoc/5/export/cypher/#export-cypher-neo4j-browser

CREATE CONSTRAINT UNIQUE_IMPORT_NAME FOR (node:`UNIQUE IMPORT LABEL`) REQUIRE (node.`UNIQUE IMPORT ID`) IS UNIQUE;
UNWIND [{_id:5, properties:{title:"redirectB", pageId:"18"}}, {_id:6, properties:{title:"redirectC", pageId:"19"}}, {_id:7, properties:{title:"stormcrow", pageId:"25"}}, {_id:9, properties:{title:"the bright lord", pageId:"27"}}, {_id:11, properties:{title:"test_redirect", pageId:"29"}}, {_id:13, properties:{title:"mithrandir", pageId:"4"}}, {_id:14, properties:{title:"the grey wizard", pageId:"5"}}, {_id:16, properties:{title:"the dark lord", pageId:"7"}}] AS row
CREATE (n:`UNIQUE IMPORT LABEL`{`UNIQUE IMPORT ID`: row._id}) SET n += row.properties SET n:redirect;
UNWIND [{_id:546, properties:{metaId:"1", property:"dump", value:"{\"lang\":\"en\",\"date\":\"11111111\",\"url\":\"https://dumps.wikimedia.org/enwiki/11111111\"}"}}] AS row
CREATE (n:`UNIQUE IMPORT LABEL`{`UNIQUE IMPORT ID`: row._id}) SET n += row.properties SET n:meta;
UNWIND [{_id:0, properties:{title:"morgoth", pageId:"10"}}, {_id:1, properties:{title:"wizard", pageId:"11"}}, {_id:2, properties:{title:"good", pageId:"12"}}, {_id:3, properties:{title:"evil", pageId:"13"}}, {_id:4, properties:{title:"wisdom", pageId:"15"}}, {_id:8, properties:{title:"bombadil", pageId:"26"}}, {_id:10, properties:{title:"test_node", pageId:"28"}}, {_id:12, properties:{title:"gandalf", pageId:"3"}}, {_id:15, properties:{title:"sauron", pageId:"6"}}, {_id:17, properties:{title:"celebrimbor", pageId:"9"}}] AS row
CREATE (n:`UNIQUE IMPORT LABEL`{`UNIQUE IMPORT ID`: row._id}) SET n += row.properties SET n:page;
UNWIND [{_id:1092, properties:{title:"aspects", categoryId:"3"}}, {_id:1093, properties:{title:"gods", categoryId:"2"}}, {_id:1094, properties:{title:"wizards", categoryId:"1"}}] AS row
CREATE (n:`UNIQUE IMPORT LABEL`{`UNIQUE IMPORT ID`: row._id}) SET n += row.properties SET n:category;
UNWIND [{start: {_id:1094}, end: {_id:13}, properties:{}}, {start: {_id:1094}, end: {_id:14}, properties:{}}, {start: {_id:1093}, end: {_id:16}, properties:{}}] AS row
MATCH (start:`UNIQUE IMPORT LABEL`{`UNIQUE IMPORT ID`: row.start._id})
MATCH (end:`UNIQUE IMPORT LABEL`{`UNIQUE IMPORT ID`: row.end._id})
CREATE (start)-[r:contains]->(end) SET r += row.properties;
UNWIND [{start: {_id:0}, end: {_id:15}, properties:{}}, {start: {_id:1}, end: {_id:2}, properties:{}}, {start: {_id:1}, end: {_id:4}, properties:{}}, {start: {_id:10}, end: {_id:17}, properties:{}}, {start: {_id:12}, end: {_id:1}, properties:{}}, {start: {_id:15}, end: {_id:3}, properties:{}}] AS row
MATCH (start:`UNIQUE IMPORT LABEL`{`UNIQUE IMPORT ID`: row.start._id})
MATCH (end:`UNIQUE IMPORT LABEL`{`UNIQUE IMPORT ID`: row.end._id})
CREATE (start)-[r:link_to]->(end) SET r += row.properties;
UNWIND [{start: {_id:13}, end: {_id:1094}, properties:{}}, {start: {_id:14}, end: {_id:1094}, properties:{}}, {start: {_id:16}, end: {_id:1093}, properties:{}}] AS row
MATCH (start:`UNIQUE IMPORT LABEL`{`UNIQUE IMPORT ID`: row.start._id})
MATCH (end:`UNIQUE IMPORT LABEL`{`UNIQUE IMPORT ID`: row.end._id})
CREATE (start)-[r:belong_to]->(end) SET r += row.properties;
UNWIND [{start: {_id:0}, end: {_id:7}, properties:{}}, {start: {_id:0}, end: {_id:16}, properties:{}}, {start: {_id:1}, end: {_id:11}, properties:{}}, {start: {_id:2}, end: {_id:9}, properties:{}}, {start: {_id:3}, end: {_id:13}, properties:{}}, {start: {_id:4}, end: {_id:9}, properties:{}}, {start: {_id:8}, end: {_id:14}, properties:{}}] AS row
MATCH (start:`UNIQUE IMPORT LABEL`{`UNIQUE IMPORT ID`: row.start._id})
MATCH (end:`UNIQUE IMPORT LABEL`{`UNIQUE IMPORT ID`: row.end._id})
CREATE (start)-[r:link_to]->(end) SET r += row.properties;
UNWIND [{start: {_id:5}, end: {_id:6}, properties:{}}, {start: {_id:6}, end: {_id:5}, properties:{}}, {start: {_id:7}, end: {_id:13}, properties:{}}] AS row
MATCH (start:`UNIQUE IMPORT LABEL`{`UNIQUE IMPORT ID`: row.start._id})
MATCH (end:`UNIQUE IMPORT LABEL`{`UNIQUE IMPORT ID`: row.end._id})
CREATE (start)-[r:redirect_to]->(end) SET r += row.properties;
UNWIND [{start: {_id:1094}, end: {_id:12}, properties:{}}, {start: {_id:1093}, end: {_id:0}, properties:{}}, {start: {_id:1093}, end: {_id:15}, properties:{}}, {start: {_id:1092}, end: {_id:2}, properties:{}}, {start: {_id:1092}, end: {_id:3}, properties:{}}] AS row
MATCH (start:`UNIQUE IMPORT LABEL`{`UNIQUE IMPORT ID`: row.start._id})
MATCH (end:`UNIQUE IMPORT LABEL`{`UNIQUE IMPORT ID`: row.end._id})
CREATE (start)-[r:contains]->(end) SET r += row.properties;
UNWIND [{start: {_id:9}, end: {_id:17}, properties:{}}, {start: {_id:11}, end: {_id:10}, properties:{}}, {start: {_id:13}, end: {_id:12}, properties:{}}, {start: {_id:14}, end: {_id:12}, properties:{}}, {start: {_id:16}, end: {_id:15}, properties:{}}] AS row
MATCH (start:`UNIQUE IMPORT LABEL`{`UNIQUE IMPORT ID`: row.start._id})
MATCH (end:`UNIQUE IMPORT LABEL`{`UNIQUE IMPORT ID`: row.end._id})
CREATE (start)-[r:redirect_to]->(end) SET r += row.properties;
UNWIND [{start: {_id:0}, end: {_id:1093}, properties:{}}, {start: {_id:2}, end: {_id:1092}, properties:{}}, {start: {_id:3}, end: {_id:1092}, properties:{}}, {start: {_id:12}, end: {_id:1094}, properties:{}}, {start: {_id:15}, end: {_id:1093}, properties:{}}] AS row
MATCH (start:`UNIQUE IMPORT LABEL`{`UNIQUE IMPORT ID`: row.start._id})
MATCH (end:`UNIQUE IMPORT LABEL`{`UNIQUE IMPORT ID`: row.end._id})
CREATE (start)-[r:belong_to]->(end) SET r += row.properties;
MATCH (n:`UNIQUE IMPORT LABEL`)  WITH n LIMIT 20000 REMOVE n:`UNIQUE IMPORT LABEL` REMOVE n.`UNIQUE IMPORT ID`;
DROP CONSTRAINT UNIQUE_IMPORT_NAME;
