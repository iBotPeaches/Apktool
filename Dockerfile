FROM gradle:8.11.1-jdk17-alpine AS build

WORKDIR /src

COPY . .

RUN gradle build shadowJar proguard


FROM openjdk:11

WORKDIR /data

COPY --from=build /src/brut.apktool/apktool-cli/build/libs/apktool-cli.jar /

ENTRYPOINT ["java", "-Xmx1024M", "-Dfile.encoding=utf-8", "-Djdk.util.zip.disableZip64ExtraFieldValidation=true", "-Djdk.nio.zipfs.allowDotZipEntry=true", "-jar", "/apktool-cli.jar"]
