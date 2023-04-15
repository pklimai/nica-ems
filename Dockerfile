FROM azul/zulu-openjdk:17
EXPOSE 8080:8080
RUN mkdir /app
COPY ./build/install/nica-ems/ /app/
COPY ./build/processedResources/jvm/main/ /app/resources/main
WORKDIR /app/bin
CMD ["./nica-ems"]
