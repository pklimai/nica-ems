FROM openjdk:8-jdk
EXPOSE 8080:8080
RUN mkdir /app
COPY ./build/install/nica-emd/ /app/
COPY ./build/resources /app/resources
WORKDIR /app/bin
CMD ["./nica-emd"]