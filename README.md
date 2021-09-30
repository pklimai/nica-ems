
# NICA Event Metadata System (EMD) API and Web UI

### About

This software is part of NICA Event Metadata System, providing REST API and Web UI for the experimental 
event catalog. PostgreSQL database is currently used for event metadata storage. Integration with Condition
database (containing experimental run metadata) and FreeIPA authorization is implemented.

### Config file

The system is rather flexible and configured using YAML file. The exact set of metadata that is
stored per experimental event is also configurable. See `resources/event-config-example.yaml` file 
for example EMD system configuration.

In the config file, you must provide credentials for EMD database and (optionally) Condition database, 
LDAP server parameters (also optional, use if you want to authenticate user queries) and specify URLs 
and parameters stored in EMD catalogue.
You can have more than one page corresponding to different metadata tables in the same EMD database.
Each page has its own URL for Web and API endpoint.

Supported parameter types are currently: `int`, `float`, `string`, `bool`.

Ranges for `int` and `float` types are supported (both in Web interface and API) using `-` 
(e.g. `track-number=10-15`). 

### API

#### Methods supported

Note: API paths are relative to `api_url` in the configuration file.

##### Get event metadata:
`GET /emd[?parameter1=value1[&parameter2=value2[...]]]`
  
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

##### Create event records in the metadata catalog:
`POST /emd`

Message body must contain the JSON list of events using format as given below.  

##### TODO: Delete event records from the metadata catalog
`DELETE /emd`

Message body must contain the JSON list of events (only `reference:` part required).

##### TODO: Count number of entries in EMD and return just this value
`GET /count[?parameter1=value1[&parameter2=value2[...]]]`

##### TODO: Get event records as a ROOT file (synchronous)
`GET /eventFile[?parameter1=value1[&parameter2=value2[...]]]`

File is built and downloaded immediately (same HTTP session) 

##### TODO: Get event records as a ROOT file reference (asynchronous)
`GET /eventFileRef[?parameter1=value1[&parameter2=value2[...]]]`

Returns the path to generated file, OR need to initially provide file path in request?

#### Event JSON schema

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

Note: `software_version` and `storage_name` must exist in the corresponding EMD database tables.
The `file_path` will be created in the `file_` table, if not there yet.

### Deploying and Testing

##### Run in Docker on CentOS 8:

```
sudo dnf install java-11-openjdk-devel
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-11.0.12.0.7-0.el8_4.x86_64
cd ~/nica-emd/
sh gradlew installDist
sudo docker build -t nica-emd .
sudo docker stop nica-emd
sudo docker rm nica-emd
sudo docker run -d --name nica-emd -p 80:8080 -v ~/nica-emd-config.yaml:/root/event-config.yaml nica-emd
```

##### Testing

Use `testing/docker-compose.yaml` for test databases. 

To test FreeIPA (LDAP), you can configure SSH tunneling such as `127.0.0.1:3890 -> bmn-ipa.jinr.ru:389`

