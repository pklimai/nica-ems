
# NICA Event Metadata System (EMS) API and Web UI

## About

This software is part of NICA Event Metadata System, providing REST API and Web UI for the experimental 
event catalog. PostgreSQL database is currently used for event metadata storage. Integration with Condition
database (containing experimental run metadata) and KeyCloak authorization is implemented.

## Config file

The system is rather flexible and is configurable via YAML file. The exact set of metadata that is
stored per experimental event is also configurable. The configuration must be passed via file named
`./ems.config.yaml`. See `./ems.config.example.yaml` file for example EMS system configuration.

In the config file, you must provide credentials for EMS database and (optionally) Condition database, 
KeyCloak server parameters (also optional, use if you want to authenticate user queries) and specify URLs 
and parameters stored in EMS catalogue.

Supported parameter types are currently: `int`, `float`, `string`, `bool`.

Ranges for `int` and `float` types are supported (both in Web interface and API) using `|` separator 
(for example `track-number=10|15`). Such range is inclusive (that is, start and end of an interval are included).
Intervals unbound from one side are also supported (for example, `track-number=10|` or `track-number=|15`).

## API

### Methods supported

#### Get event metadata:
`GET /event_api/v1/event[?parameter1=value1[&parameter2=value2[...]]]`
  
Here and below, for parameter values we have 
* Standard parameters:
  - `software_version` (string)
  - `period_number` (short)
  - `run_number` (int)

* Condition database preselection parameters:
  - `beam_particle` (string)
  - `target_particle` (string)
  - `energy` (float)

* Limit number of records returned, and provide offset for first entry:
  - `limit` (int)
  - `offset` (int)

* Any custom parameters specified in YAML file 

Example:
`http://127.0.0.1:8080/event_api/v1/event?limit=5&software_version=~19._&beam_particle=~A_&track_number=11|`


#### Create event records in the metadata catalog:
`POST /event_api/v1/event`

Message body must contain the JSON list of events using format as given below.  

#### TODO: Delete event records from the metadata catalog
`DELETE /event_api/v1/event`

Message body must contain the JSON list of events (only `reference:` part required).

#### TODO: Count number of entries in EMS and return just this value
`GET /count[?parameter1=value1[&parameter2=value2[...]]]`

#### TODO: Get event records as a ROOT file (synchronous)
`GET /event_api/v1/eventFile[?parameter1=value1[&parameter2=value2[...]]]`

File is built and downloaded immediately (same HTTP session) 

#### TODO: Get event records as a ROOT file reference (asynchronous)
`GET /event_api/v1/eventFileRef[?parameter1=value1[&parameter2=value2[...]]]`

Returns the path to generated file, OR need to initially provide file path in request?


### JSON schema for Events

Both GET and POST use the same format for events, like this:

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

For example here is how you can create events in the catalog using `curl` tool:
```
curl -X POST -u USER:PASS -H "Content-Type: application/json" http://127.0.0.1/event_api/v1/event -d '
[
{
 "reference": {
  "storage_name": "/var/tmp",
  "file_path": "1.txt",
  "event_number": 5
 },
 "software_version": "19.1",
 "period_number": 7,
 "run_number": 5001,
 "parameters": {
  "track_number": 25
 }
}
]
'
```

Note: `software_version` and `storage_name` must exist in the corresponding EMS database tables.
The `file_path` will be created in the `file_` table, if not there yet.


## Deploying and Testing

### Run in Docker on CentOS / Alma Linux example:

```
#sudo dnf install java-11-openjdk-devel
#export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-11.0.12.0.7-0.el8_4.x86_64
sudo yum install java-17
cd ~/nica-ems/
sh gradlew installDist
docker build -t nica-ems:current .
# sudo docker stop nica-ems
# sudo docker rm nica-ems
sudo docker run -d --rm --name nica-ems -p 80:8080 -v ~/ems.config.yaml:/app/bin/ems.config.yaml nica-ems:current
```

### Run on Alma Linux

```
sudo yum install java-17
git clone https://github.com/pklimai/nica-ems
cd nica-ems/
sh gradlew run
```

### Dockerfile with build

To build inside Docker and run, use e.g. (note: Dockerfile.with-build uses 2-stage build to optimize 
resulting container image size):
```
sudo docker build -f Dockerfile.with-build -t nica-ems:buildindocker .
sudo docker run --rm -it -v ~/ems.config.yaml:/app/bin/ems.config.yaml -p 80:8080 nica-ems:buildindocker
```

### Testing / debugging

Use `testing/docker-compose.yaml` for test databases. 

To test FreeIPA (LDAP), you can configure SSH tunneling such as `127.0.0.1:3890 -> bmn-ipa.jinr.ru:389`

To view application logs:
```
docker logs nica-ems -f
```

To run container in debug mode, use something like
```
sudo docker run -it --entrypoint=/bin/bash --name nica-ems --rm -p 80:8080 -v ~/ems.config.yaml:/app/bin/ems.config.yaml nica-ems:current
```


