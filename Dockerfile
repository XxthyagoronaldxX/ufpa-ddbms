FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY . /app

RUN ./mvnw clean package

CMD java -jar target/ddbms-1.0-SNAPSHOT.jar 8080 --id=${NODE_ID}