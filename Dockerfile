FROM eclipse-temurin:24-jre-alpine AS builder
WORKDIR /workspace
COPY ./build/libs/*.jar app.jar
RUN mkdir extracted
RUN java -Djarmode=tools -jar app.jar extract --layers --destination extracted --launcher
FROM eclipse-temurin:24-jre-alpine
RUN apk update && apk upgrade && apk add curl jq && apk cache clean && rm -rf /var/cache/apk/*
WORKDIR /application
COPY --from=builder /workspace/extracted/dependencies/ ./
COPY --from=builder /workspace/extracted/spring-boot-loader/ ./
COPY --from=builder /workspace/extracted/snapshot-dependencies/ ./
COPY --from=builder /workspace/extracted/application/ ./
EXPOSE 80
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
