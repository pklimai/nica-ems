
Run in Docker on CentOS 8:

```
sudo dnf install java-11-openjdk-devel
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-11.0.9.11-3.el8_3.x86_64
cd ~/ktor-test/
./gradlew installDist
sudo docker build -t ktor-test .
sudo docker run -p 80:8080  ktor-test
```

