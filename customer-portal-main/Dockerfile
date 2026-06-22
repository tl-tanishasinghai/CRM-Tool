ARG MAVEN_BASE_IMAGE=352484409731.dkr.ecr.ap-south-1.amazonaws.com/tl-base-image-maven:v1.4
ARG JAVA_BASE_IMAGE=352484409731.dkr.ecr.ap-south-1.amazonaws.com/tl-base-image-java:v1.4
FROM ${MAVEN_BASE_IMAGE} AS java-build
LABEL name="customer-portal-service" maintainer="Pratyush Shrivastav"
# jar packaging
COPY . /app/
RUN mvn clean package -Dmaven.test.skip=true

FROM ${JAVA_BASE_IMAGE}
COPY --from=java-build /app/target/*.jar /app/customer-portal.jar

ENV JAVA_OPTS="\
  -XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=65.0 \
  -XX:+UnlockDiagnosticVMOptions \
  -XX:NativeMemoryTracking=summary"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Dspring.profiles.active=${PROFILE} -javaagent:/app/opentelemetry-javaagent.jar -Duser.timezone=Asia/Kolkata -jar /app/customer-portal.jar"]