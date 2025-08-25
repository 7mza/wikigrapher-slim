// https://neo4j.com/docs/apoc/2025.07/export/cypher/

CREATE CONSTRAINT UNIQUE_IMPORT_NAME FOR (node:`UNIQUE IMPORT LABEL`) REQUIRE (node.`UNIQUE IMPORT ID`) IS UNIQUE;
UNWIND [{_id:5, properties:{title:"nothing", pageId:"16"}}, {_id:6, properties:{title:"redirectA", pageId:"17"}}, {_id:7, properties:{title:"redirectB", pageId:"18"}}, {_id:8, properties:{title:"redirectC", pageId:"19"}}, {_id:9, properties:{title:"stormcrow", pageId:"25"}}, {_id:11, properties:{title:"the bright lord", pageId:"27"}}, {_id:13, properties:{title:"test_redirect", pageId:"29"}}, {_id:16, properties:{title:"mithrandir", pageId:"4"}}, {_id:17, properties:{title:"the grey wizard", pageId:"5"}}, {_id:19, properties:{title:"the dark lord", pageId:"7"}}, {_id:20, properties:{title:"the necromancer", pageId:"8"}}] AS row
CREATE (n:`UNIQUE IMPORT LABEL`{`UNIQUE IMPORT ID`: row._id}) SET n += row.properties SET n:redirect;
UNWIND [{_id:1092, properties:{metaId:"1", property:"dump", value:"{\"lang\":\"en\",\"date\":\"11111111\",\"url\":\"https://dumps.wikimedia.org/enwiki/11111111\"}"}}] AS row
CREATE (n:`UNIQUE IMPORT LABEL`{`UNIQUE IMPORT ID`: row._id}) SET n += row.properties SET n:meta;
UNWIND [{_id:0, properties:{title:"morgoth", pageId:"10"}}, {_id:1, properties:{title:"wizard", pageId:"11"}}, {_id:2, properties:{title:"good", pageId:"12"}}, {_id:3, properties:{title:"evil", pageId:"13"}}, {_id:4, properties:{title:"wisdom", pageId:"15"}}, {_id:10, properties:{title:"bombadil", pageId:"26"}}, {_id:12, properties:{title:"test_node", pageId:"28"}}, {_id:14, properties:{title:"gandalf", pageId:"3"}}, {_id:15, properties:{title:"orphan1", pageId:"30"}}, {_id:18, properties:{title:"sauron", pageId:"6"}}, {_id:21, properties:{title:"celebrimbor", pageId:"9"}}] AS row
CREATE (n:`UNIQUE IMPORT LABEL`{`UNIQUE IMPORT ID`: row._id}) SET n += row.properties SET n:page;
UNWIND [{_id:22, properties:{createdAt:1756084494577, id:"16", type:"redirect", title:"nothing"}}, {_id:23, properties:{createdAt:1756084494577, id:"30", type:"page", title:"orphan1"}}, {_id:24, properties:{createdAt:1756084494577, id:"8", type:"redirect", title:"the necromancer"}}] AS row
CREATE (n:`UNIQUE IMPORT LABEL`{`UNIQUE IMPORT ID`: row._id}) SET n += row.properties SET n:orphan;
UNWIND [{_id:546, properties:{title:"aspects", categoryId:"3"}}, {_id:547, properties:{title:"gods", categoryId:"2"}}, {_id:548, properties:{title:"wizards", categoryId:"1"}}] AS row
CREATE (n:`UNIQUE IMPORT LABEL`{`UNIQUE IMPORT ID`: row._id}) SET n += row.properties SET n:category;
UNWIND [{start: {_id:548}, end: {_id:16}, properties:{}}, {start: {_id:548}, end: {_id:17}, properties:{}}, {start: {_id:547}, end: {_id:19}, properties:{}}, {start: {_id:547}, end: {_id:20}, properties:{}}, {start: {_id:546}, end: {_id:5}, properties:{}}] AS row
MATCH (start:`UNIQUE IMPORT LABEL`{`UNIQUE IMPORT ID`: row.start._id})
MATCH (end:`UNIQUE IMPORT LABEL`{`UNIQUE IMPORT ID`: row.end._id})
CREATE (start)-[r:contains]->(end) SET r += row.properties;
UNWIND [{start: {_id:0}, end: {_id:18}, properties:{}}, {start: {_id:1}, end: {_id:2}, properties:{}}, {start: {_id:1}, end: {_id:4}, properties:{}}, {start: {_id:12}, end: {_id:21}, properties:{}}, {start: {_id:14}, end: {_id:1}, properties:{}}, {start: {_id:18}, end: {_id:3}, properties:{}}] AS row
MATCH (start:`UNIQUE IMPORT LABEL`{`UNIQUE IMPORT ID`: row.start._id})
MATCH (end:`UNIQUE IMPORT LABEL`{`UNIQUE IMPORT ID`: row.end._id})
CREATE (start)-[r:link_to]->(end) SET r += row.properties;
UNWIND [{start: {_id:5}, end: {_id:546}, properties:{}}, {start: {_id:16}, end: {_id:548}, properties:{}}, {start: {_id:17}, end: {_id:548}, properties:{}}, {start: {_id:19}, end: {_id:547}, properties:{}}, {start: {_id:20}, end: {_id:547}, properties:{}}] AS row
MATCH (start:`UNIQUE IMPORT LABEL`{`UNIQUE IMPORT ID`: row.start._id})
MATCH (end:`UNIQUE IMPORT LABEL`{`UNIQUE IMPORT ID`: row.end._id})
CREATE (start)-[r:belong_to]->(end) SET r += row.properties;
UNWIND [{start: {_id:0}, end: {_id:9}, properties:{}}, {start: {_id:0}, end: {_id:19}, properties:{}}, {start: {_id:1}, end: {_id:13}, properties:{}}, {start: {_id:2}, end: {_id:11}, properties:{}}, {start: {_id:3}, end: {_id:16}, properties:{}}, {start: {_id:4}, end: {_id:11}, properties:{}}, {start: {_id:10}, end: {_id:17}, properties:{}}, {start: {_id:14}, end: {_id:6}, properties:{}}] AS row
MATCH (start:`UNIQUE IMPORT LABEL`{`UNIQUE IMPORT ID`: row.start._id})
MATCH (end:`UNIQUE IMPORT LABEL`{`UNIQUE IMPORT ID`: row.end._id})
CREATE (start)-[r:link_to]->(end) SET r += row.properties;
UNWIND [{start: {_id:7}, end: {_id:8}, properties:{}}, {start: {_id:8}, end: {_id:7}, properties:{}}, {start: {_id:9}, end: {_id:16}, properties:{}}] AS row
MATCH (start:`UNIQUE IMPORT LABEL`{`UNIQUE IMPORT ID`: row.start._id})
MATCH (end:`UNIQUE IMPORT LABEL`{`UNIQUE IMPORT ID`: row.end._id})
CREATE (start)-[r:redirect_to]->(end) SET r += row.properties;
UNWIND [{start: {_id:548}, end: {_id:14}, properties:{}}, {start: {_id:547}, end: {_id:0}, properties:{}}, {start: {_id:547}, end: {_id:18}, properties:{}}, {start: {_id:546}, end: {_id:2}, properties:{}}, {start: {_id:546}, end: {_id:3}, properties:{}}] AS row
MATCH (start:`UNIQUE IMPORT LABEL`{`UNIQUE IMPORT ID`: row.start._id})
MATCH (end:`UNIQUE IMPORT LABEL`{`UNIQUE IMPORT ID`: row.end._id})
CREATE (start)-[r:contains]->(end) SET r += row.properties;
UNWIND [{start: {_id:11}, end: {_id:21}, properties:{}}, {start: {_id:13}, end: {_id:12}, properties:{}}, {start: {_id:16}, end: {_id:14}, properties:{}}, {start: {_id:17}, end: {_id:14}, properties:{}}, {start: {_id:19}, end: {_id:18}, properties:{}}] AS row
MATCH (start:`UNIQUE IMPORT LABEL`{`UNIQUE IMPORT ID`: row.start._id})
MATCH (end:`UNIQUE IMPORT LABEL`{`UNIQUE IMPORT ID`: row.end._id})
CREATE (start)-[r:redirect_to]->(end) SET r += row.properties;
UNWIND [{start: {_id:0}, end: {_id:547}, properties:{}}, {start: {_id:2}, end: {_id:546}, properties:{}}, {start: {_id:3}, end: {_id:546}, properties:{}}, {start: {_id:14}, end: {_id:548}, properties:{}}, {start: {_id:18}, end: {_id:547}, properties:{}}] AS row
MATCH (start:`UNIQUE IMPORT LABEL`{`UNIQUE IMPORT ID`: row.start._id})
MATCH (end:`UNIQUE IMPORT LABEL`{`UNIQUE IMPORT ID`: row.end._id})
CREATE (start)-[r:belong_to]->(end) SET r += row.properties;
MATCH (n:`UNIQUE IMPORT LABEL`)  WITH n LIMIT 20000 REMOVE n:`UNIQUE IMPORT LABEL` REMOVE n.`UNIQUE IMPORT ID`;
DROP CONSTRAINT UNIQUE_IMPORT_NAME;
