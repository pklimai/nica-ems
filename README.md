
# NICA Event Metadata System (EMS) API and Web UI

## About

This software is part of the NICA Event Metadata System, providing REST API and Web User Interface (UI) for the 
Event Catalogue of an experiment on particle collisions. PostgreSQL database is currently used for event metadata storage. Integration with the [Condition Database](https://git.jinr.ru/nica_db/unidb_platform) (containing run metadata of an experiment) and KeyCloak authorization is implemented.

## Deployment

### Setting up the configuration file

The system is configurable via YAML file for a particular experiment on particle collisions, including a set of specific metadata that are stored per experiment event. The configuration is defined in the file named `ems.config.yaml`. An example of the EMS system configuration for the BM@N experiment can be seen in the `ems.bmn-config.yaml` file.

In the configuration file, you must provide credentials for the Event Database and (optionally) Condition database, KeyCloak server parameters (also optional) and specify URLs and parameters stored in the EMS Catalogue.

Supported parameter types are currently: `int`, `float`, `string`, `bool`. Ranges for `int` and `float` types are supported (both in Web interface and API) using `|` separator 
(for instance, `track-number=10|15`). Such range is inclusive (that is, start and end of an interval are included). Intervals unbound from one side are also supported (for example, `track-number=10|` or `track-number=|15`).

### Run installation of the system on a RedHat-based Operating System (AlmaLinux, CentOS, RedHat)

There are three possible ways to install the EMS API and Web UI.

- Installation and run inside Docker-container (recommended):
```
sudo yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
sudo yum install -y docker-ce docker-ce-cli containerd.io git
sudo systemctl enable --now docker
sudo systemctl status docker

git clone https://git.jinr.ru/nica_db/emd.git
cd emd/
sudo docker build -f Dockerfile.with-build -t nica-ems:buildindocker .
sudo docker run --rm -it -v ./ems.config.yaml:/app/bin/ems.config.yaml -p 80:8080 nica-ems:buildindocker
```

- Local installation (for development purpose):
```
sudo yum -y install java-17 git gcc-c++
git clone https://git.jinr.ru/nica_db/emd.git
cd emd/
sh gradlew run
```

- Local installation but run inside the Docker-container (for development purpose):
```
sudo yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
sudo yum install -y docker-ce docker-ce-cli containerd.io java-17 git gcc-c++
sudo systemctl enable --now docker
sudo systemctl status docker

git clone https://git.jinr.ru/nica_db/emd.git
cd emd/
sh gradlew installDist
docker build -t nica-ems:current .
sudo docker run -d --rm --name nica-ems -p 80:8080 -v ./ems.config.yaml:/app/bin/ems.config.yaml nica-ems:current
```

## Using deployed REST API service

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
