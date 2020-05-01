FROM openjdk:11.0-jre-slim
VOLUME /tmp
COPY target/lib/ ./lib/
ADD target/*.jar app.jar
RUN sh -c 'touch app.jar'
ENV JVM_OPTS="-Xss256k -Duser.timezone=Asia/Shanghai -Djava.security.egd=file:/dev/./urandom"
ENV JAVA_OPTS=""
ENV APP_OPTS=""
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS  $JVM_OPTS  -jar app.jar $APP_OPTS" ]