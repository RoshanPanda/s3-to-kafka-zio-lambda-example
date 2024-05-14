FROM amazoncorretto:11

# Set environment variables
ENV JAVA_OPTS="-Xmx512m -Xms512m"
ENV SERVER_PORT=8083

# Set the working directory
WORKDIR /mw-wmos-to-pe-flow


# Copy the JAR file to the container
COPY target/scala*/*.jar mw-wmos-to-pe-flow.jar

# Run the application using shell form of ENTRYPOINT
ENTRYPOINT java ${JAVA_OPTS} -jar mw-wmos-to-pe-flow.jar --server.port=${SERVER_PORT}

# Expose the server port
EXPOSE ${SERVER_PORT}