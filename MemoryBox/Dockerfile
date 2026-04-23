FROM eclipse-temurin:21-jre

RUN apt-get update \
    && apt-get install -y --no-install-recommends ffmpeg \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

ARG WAR_FILE=target/*.war
COPY ${WAR_FILE} app.war

ENV APP_FFMPEG_COMMAND=ffmpeg
ENV APP_STORAGE_LOCAL_ROOT=/data/upload/

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.war", "--app.ffmpeg.command=${APP_FFMPEG_COMMAND}", "--app.storage.local-root=${APP_STORAGE_LOCAL_ROOT}"]
