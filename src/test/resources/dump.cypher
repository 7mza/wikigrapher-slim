// https://neo4j.com/docs/apoc/5/export/cypher/#export-cypher-neo4j-browser

CREATE CONSTRAINT UNIQUE_IMPORT_NAME FOR (node:`UNIQUE IMPORT LABEL`) REQUIRE (node.`UNIQUE IMPORT ID`) IS UNIQUE;
UNWIND [{_id:551, properties:{title:"redirectB", pageId:"18"}}, {_id:552, properties:{title:"redirectC", pageId:"19"}}, {_id:553, properties:{title:"stormcrow", pageId:"25"}}, {_id:555, properties:{title:"the bright lord", pageId:"27"}}, {_id:557, properties:{title:"mithrandir", pageId:"4"}}, {_id:558, properties:{title:"the grey wizard", pageId:"5"}}, {_id:560, properties:{title:"the dark lord", pageId:"7"}}] AS row
CREATE (n:`UNIQUE IMPORT LABEL`{`UNIQUE IMPORT ID`: row._id}) SET n += row.properties SET n:redirect;
UNWIND [{_id:1092, properties:{metaId:"1", property:"dump", value:"{\"lang\":\"en\",\"date\":\"11111111\",\"url\":\"https://dumps.wikimedia.org/enwiki/11111111\"}"}}] AS row
CREATE (n:`UNIQUE IMPORT LABEL`{`UNIQUE IMPORT ID`: row._id}) SET n += row.properties SET n:meta;
UNWIND [{_id:546, properties:{title:"morgoth", pageId:"10"}}, {_id:547, properties:{title:"wizard", pageId:"11"}}, {_id:548, properties:{title:"good", pageId:"12"}}, {_id:549, properties:{title:"evil", pageId:"13"}}, {_id:550, properties:{title:"wisdom", pageId:"15"}}, {_id:554, properties:{title:"bombadil", pageId:"26"}}, {_id:556, properties:{title:"gandalf", pageId:"3"}}, {_id:559, properties:{title:"sauron", pageId:"6"}}, {_id:561, properties:{title:"celebrimbor", pageId:"9"}}] AS row
CREATE (n:`UNIQUE IMPORT LABEL`{`UNIQUE IMPORT ID`: row._id}) SET n += row.properties SET n:page;
UNWIND [{_id:0, properties:{title:"aspects", categoryId:"3"}}, {_id:1, properties:{title:"gods", categoryId:"2"}}, {_id:2, properties:{title:"wizards", categoryId:"1"}}] AS row
CREATE (n:`UNIQUE IMPORT LABEL`{`UNIQUE IMPORT ID`: row._id}) SET n += row.properties SET n:category;
UNWIND [{start: {_id:2}, end: {_id:557}, properties:{}}, {start: {_id:2}, end: {_id:558}, properties:{}}, {start: {_id:1}, end: {_id:560}, properties:{}}] AS row
MATCH (start:`UNIQUE IMPORT LABEL`{`UNIQUE IMPORT ID`: row.start._id})
MATCH (end:`UNIQUE IMPORT LABEL`{`UNIQUE IMPORT ID`: row.end._id})
CREATE (start)-[r:contains]->(end) SET r += row.properties;
UNWIND [{start: {_id:546}, end: {_id:559}, properties:{}}, {start: {_id:547}, end: {_id:548}, properties:{}}, {start: {_id:547}, end: {_id:550}, properties:{}}, {start: {_id:556}, end: {_id:547}, properties:{}}, {start: {_id:559}, end: {_id:549}, properties:{}}] AS row
MATCH (start:`UNIQUE IMPORT LABEL`{`UNIQUE IMPORT ID`: row.start._id})
MATCH (end:`UNIQUE IMPORT LABEL`{`UNIQUE IMPORT ID`: row.end._id})
CREATE (start)-[r:link_to]->(end) SET r += row.properties;
UNWIND [{start: {_id:557}, end: {_id:2}, properties:{}}, {start: {_id:558}, end: {_id:2}, properties:{}}, {start: {_id:560}, end: {_id:1}, properties:{}}] AS row
MATCH (start:`UNIQUE IMPORT LABEL`{`UNIQUE IMPORT ID`: row.start._id})
MATCH (end:`UNIQUE IMPORT LABEL`{`UNIQUE IMPORT ID`: row.end._id})
CREATE (start)-[r:belong_to]->(end) SET r += row.properties;
UNWIND [{start: {_id:546}, end: {_id:553}, properties:{}}, {start: {_id:546}, end: {_id:560}, properties:{}}, {start: {_id:548}, end: {_id:555}, properties:{}}, {start: {_id:549}, end: {_id:557}, properties:{}}, {start: {_id:550}, end: {_id:555}, properties:{}}, {start: {_id:554}, end: {_id:558}, properties:{}}] AS row
MATCH (start:`UNIQUE IMPORT LABEL`{`UNIQUE IMPORT ID`: row.start._id})
MATCH (end:`UNIQUE IMPORT LABEL`{`UNIQUE IMPORT ID`: row.end._id})
CREATE (start)-[r:link_to]->(end) SET r += row.properties;
UNWIND [{start: {_id:551}, end: {_id:552}, properties:{}}, {start: {_id:552}, end: {_id:551}, properties:{}}, {start: {_id:553}, end: {_id:557}, properties:{}}] AS row
MATCH (start:`UNIQUE IMPORT LABEL`{`UNIQUE IMPORT ID`: row.start._id})
MATCH (end:`UNIQUE IMPORT LABEL`{`UNIQUE IMPORT ID`: row.end._id})
CREATE (start)-[r:redirect_to]->(end) SET r += row.properties;
UNWIND [{start: {_id:2}, end: {_id:556}, properties:{}}, {start: {_id:1}, end: {_id:546}, properties:{}}, {start: {_id:1}, end: {_id:559}, properties:{}}, {start: {_id:0}, end: {_id:548}, properties:{}}, {start: {_id:0}, end: {_id:549}, properties:{}}] AS row
MATCH (start:`UNIQUE IMPORT LABEL`{`UNIQUE IMPORT ID`: row.start._id})
MATCH (end:`UNIQUE IMPORT LABEL`{`UNIQUE IMPORT ID`: row.end._id})
CREATE (start)-[r:contains]->(end) SET r += row.properties;
UNWIND [{start: {_id:555}, end: {_id:561}, properties:{}}, {start: {_id:557}, end: {_id:556}, properties:{}}, {start: {_id:558}, end: {_id:556}, properties:{}}, {start: {_id:560}, end: {_id:559}, properties:{}}] AS row
MATCH (start:`UNIQUE IMPORT LABEL`{`UNIQUE IMPORT ID`: row.start._id})
MATCH (end:`UNIQUE IMPORT LABEL`{`UNIQUE IMPORT ID`: row.end._id})
CREATE (start)-[r:redirect_to]->(end) SET r += row.properties;
UNWIND [{start: {_id:546}, end: {_id:1}, properties:{}}, {start: {_id:548}, end: {_id:0}, properties:{}}, {start: {_id:549}, end: {_id:0}, properties:{}}, {start: {_id:556}, end: {_id:2}, properties:{}}, {start: {_id:559}, end: {_id:1}, properties:{}}] AS row
MATCH (start:`UNIQUE IMPORT LABEL`{`UNIQUE IMPORT ID`: row.start._id})
MATCH (end:`UNIQUE IMPORT LABEL`{`UNIQUE IMPORT ID`: row.end._id})
CREATE (start)-[r:belong_to]->(end) SET r += row.properties;
MATCH (n:`UNIQUE IMPORT LABEL`)  WITH n LIMIT 20000 REMOVE n:`UNIQUE IMPORT LABEL` REMOVE n.`UNIQUE IMPORT ID`;
DROP CONSTRAINT UNIQUE_IMPORT_NAME;
