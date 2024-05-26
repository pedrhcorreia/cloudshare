# Use a lightweight Java runtime as base image
FROM adoptopenjdk/openjdk21:alpine-jre

# Set the working directory in the container
WORKDIR /app

# Copy the Uber JAR into the container at /app
COPY target/cloudshare-quarkus.application.version-runner.jar /app/cloudshare-quarkus.application.version-runner.jar

# Make port 8080 available to the world outside this container
EXPOSE 8080

# Define command to run the Uber JAR
CMD ["java", "-jar", "cloudshare-quarkus.application.version-runner.jar"]
