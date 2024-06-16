# Use the Ubuntu 16.04 base image
FROM ubuntu:16.04

# Update package list and install necessary packages
RUN apt-get update && apt-get -q -y install \
    curl \
    zip \
    unzip \
    git \
    openjdk-8-jdk

# Set Java 8 as the default Java version - ARM64 if going for default in Podman
# ENV JAVA_HOME /usr/lib/jvm/java-8-openjdk-amd64 if going for default in Docker
ENV JAVA_HOME /usr/lib/jvm/java-8-openjdk-arm64
ENV PATH $JAVA_HOME/bin:$PATH

# Install SDKMAN
RUN curl -s "https://get.sdkman.io" | bash

# Source SDKMAN script
SHELL ["/bin/bash", "-c"]

# Install Gradle and Groovy using SDKMAN
RUN source "$HOME/.sdkman/bin/sdkman-init.sh" && \
    sdk install gradle 6.4.1 && \
    sdk install groovy 2.5.5

# Clone the specified GitHub repository
RUN git clone https://github.com/MikeForde/openEHR-OPT.git /opt/openEHR-OPT

# Set working directory
WORKDIR /opt/openEHR-OPT

# Set PATH for Gradle and Groovy
ENV PATH $HOME/.sdkman/candidates/gradle/current/bin:$HOME/.sdkman/candidates/groovy/current/bin:$PATH

# Print JAVA_HOME and java version for debugging
RUN echo "JAVA_HOME: $JAVA_HOME" && java -version && groovy -version
