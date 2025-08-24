# <img src="./docs/wikigrapher.png" alt="drawing" width="50"/> wikigrapher-slim

explore wikipedia as a knowledge graph [https://wikigrapher.com/](https://wikigrapher.com)

standalone web app for [7mza/wikigrapher-generator](https://github.com/7mza/wikigrapher-generator)

## stack

* kotlin / spring webflux
* spring data neo4j
* thymeleaf / typescript
* bootstrap / vis network
* gradle / npm / webpack

## requirements

[sdkman](https://sdkman.io)

```shell
sdk env install
```

[nvm](https://github.com/nvm-sh/nvm)

```shell
nvm install
```

docker

## setup neo4j

refer to [7mza/wikigrapher-generator](https://github.com/7mza/wikigrapher-generator) to generate wikipedia graph & setup
neo4j

## build

```shell
npm i
```

```shell

npm run build && ./gradlew clean ktlintFormat ktlintCheck build

# or for source maps

npm run build:dev && ./gradlew clean ktlintFormat ktlintCheck build -Pmode=development
```

## run

spring is configured with compose support, run with ide or

```shell
./gradlew buildLocalDockerImage
```

```shell
docker compose up --build
```

[http://localhost:8080/](http://localhost:8080/)

## license

this project is licensed under the [GNU Affero General Public License v3.0](./LICENSE.txt)

wikipedia® is a registered trademark of the wikimedia foundation

this project is independently developed and not affiliated with or endorsed by the wikimedia foundation
