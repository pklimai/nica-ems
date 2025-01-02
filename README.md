
# API and Web UI of the Event Metadata System 

## About

This software is part of the Event Metadata System (EMS), providing REST API and Web User Interface (UI) for the 
Event Catalogue of an experiment on particle collisions. The PostgreSQL database is currently used as the event metadata storage. EMS supports integration with the [Unified Condition Database (UniConDa)](https://git.jinr.ru/nica_db/unidb_platform) containing run metadata of an experiment for fast pre-selection and KeyCloak identity provider ensuring cental authentification and authorization for users.

## Deployment

### Setting up the configuration file

The system is configurable via YAML file for a particular experiment on particle collisions, including a set of specific event metadata. The configuration should be prepared in a file named `ems.config.yaml`. An example of the EMS configuration for the BM@N experiment can be seen in the `ems.bmn-config.yaml` file.

In the configuration file, you must provide credentials for the event database (Event Catalogue) and optionally for the Unified Condition database (if it is employed), optionally set KeyCloak server parameters, and specify URLs and event parameters (metadata) stored in the Event Catalogue.

Supported event parameter types are currently: `int`, `float`, `string`, `bool`. Acceptable ranges for `int` and `float` values can be defined using `|` separator for both Web interface and API (for instance, `track-number=10|15`). The ranges are inclusive, that is start and end of the intervals are included. The intervals unbound from one side are also supported, for example, `track-number=10|` or `track-number=|15`.

### Run installation of the EMS interfaces on a RedHat-based Operating System (AlmaLinux, CentOS, RedHat)

There are three possible ways to install the EMS API and Web UI.

- Installation and run inside Docker-container (recommended):
```
sudo yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
sudo yum install -y docker-ce docker-ce-cli containerd.io git nano
sudo systemctl enable --now docker
sudo systemctl status docker

git clone https://git.jinr.ru/nica_db/emd.git
cd emd/
sudo docker build -f Dockerfile.with-build -t nica-ems:buildindocker .
nano ems.config.yaml   # set up or check your configuration file
sudo docker run --rm -it -v ./ems.config.yaml:/app/bin/ems.config.yaml -p 80:8080 nica-ems:buildindocker
```

- Local installation (for development purpose):
```
sudo yum -y install java-17 git gcc-c++ nano
git clone https://git.jinr.ru/nica_db/emd.git
cd emd/
nano ems.config.yaml   # set up or check your configuration file
sh gradlew run
```

- Local installation but run inside the Docker-container (for development purpose):
```
sudo yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
sudo yum install -y docker-ce docker-ce-cli containerd.io java-17 git gcc-c++ nano
sudo systemctl enable --now docker
sudo systemctl status docker

git clone https://git.jinr.ru/nica_db/emd.git
cd emd/
sh gradlew installDist
docker build -t nica-ems:current .
nano ems.config.yaml   # set up or check your configuration file
sudo docker run -d --rm --name nica-ems -p 80:8080 -v ./ems.config.yaml:/app/bin/ems.config.yaml nica-ems:current
```

## Using the deployed REST API service

### Methods supported

#### Read event records (get event metadata):
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

* Any custom parameters specified in YAML configuration file 

Example:
`http://127.0.0.1:8080/event_api/v1/event?limit=5&software_version=~19._&beam_particle=~A_&track_number=11|`


#### Create event records in the metadata catalog (Writer or Admin role required):
`POST /event_api/v1/event`

Message body must contain the JSON list of events to be written, using format as given below. In case of any events 
already present, an error is returned and whole transaction is cancelled.


#### Update event records in the metadata catalog (Writer or Admin role required):
`PUT /event_api/v1/event`

Message body must contain the JSON list of events to be created or updated. The request succeeds even if some records
are already present (if any parameters are different, they are updated). 


#### Delete event records from the metadata catalog (Admin role required)
`DELETE /event_api/v1/event`

Message body must contain the JSON list of events (only `reference:` part is required, other fields are
optional and ignored, if present).


#### Read software records from dictionary
`GET /event_api/v1/software`


#### Create software record in dictionary
`POST /event_api/v1/software`

Message body example `{"software_id": 100, "software_version": "22.11"}`


#### Read storage records from dictionary
`GET /event_api/v1/storage`


#### Create storage record in dictionary
`POST /event_api/v1/storage`

Message body example `{"storage_id": 100, "storage_name": "data1"}`


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
