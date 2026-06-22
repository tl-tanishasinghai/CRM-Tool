ARG MAVEN_BASE_IMAGE=352484409731.dkr.ecr.ap-south-1.amazonaws.com/tl-base-image-maven:v1.4
ARG JAVA_BASE_IMAGE=352484409731.dkr.ecr.ap-south-1.amazonaws.com/tl-base-image-java:v1.4
FROM ${MAVEN_BASE_IMAGE} AS java-build

LABEL name="lms-service" maintainer="DevOps"

COPY . /app/
RUN mvn clean package -Dmaven.test.skip=true

FROM ${JAVA_BASE_IMAGE}
COPY --from=java-build /app/target/*.jar /app/los.jar
USER trillionloans
ENTRYPOINT ["java","-Dspring.profiles.active=${PROFILE}", "-Duser.timezone=Asia/Kolkata", "-javaagent:/app/opentelemetry-javaagent.jar", "-jar", "/app/los.jar"]
