
# Event Metadata System API and Web UI

### Run in Docker on CentOS 8:

```
sudo dnf install java-11-openjdk-devel
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-11.0.9.11-3.el8_3.x86_64
cd ~/nica-emd/
./gradlew installDist
sudo docker build -t nica-emd .
sudo docker run -p 80:8080 nica-emd
```

### Testing

Use `testing/docker-compose.yaml` for test databases. 

### Event JSON example

```
[ 
  {
    "reference": {
      "storage_name": "data1",
      "file_path": "/tmp/file1",
      "event_number": 1
    },
    "software_version": "19.1",
    "period_number": 7,
    "run_number": 5000,
    "parameters": {
      "track_number": 20
    }
  } 
]
```
