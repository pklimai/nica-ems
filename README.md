
# Event Metadata System (EMS)

## API and Web UI

### About
This software is part of the Event Metadata System (EMS), providing REST API and Web User Interface (UI) for the Event Catalogue of an experiment on particle collisions. The PostgreSQL database is currently used as the event metadata storage. EMS supports integration with the [Unified Condition Database (UniConDa)](https://git.jinr.ru/nica_db/unidb_platform) containing run metadata of an experiment for fast pre-selection and KeyCloak identity provider ensuring central authentication and authorization for users.

### Deployment

#### Setting up the configuration file
The system is configurable via YAML file for a particular experiment on particle collisions, including a set of specific event metadata. The configuration should be prepared in a file named `ems.config.yaml`. An example of the EMS configuration for the BM@N experiment can be seen in the `ems.bmn-config.yaml` file.

In the configuration file:
- Provide credentials for the event database (Event Catalogue).
- Optionally provide credentials for the Unified Condition Database (if employed).
- Optionally set KeyCloak server parameters.
- Specify URLs and event parameters (metadata) stored in the Event Catalogue.

Supported event parameter types are: `int`, `float`, `string`, `bool`. Acceptable ranges for `int` and `float` values can be defined using the `|` separator for both Web interface and API (e.g., `track-number=10|15`). The ranges are inclusive, meaning start and end of the intervals are included. Unbounded intervals are also supported (e.g., `track-number=10|` or `track-number=|15`).

If you want extensive debug output (including all SQL requests), specify `debug: True` in the configuration file.

#### Installation Methods
There are three possible ways to install the EMS API and Web UI:

1. **Installation and run inside Docker-container (recommended):**
   ```bash
   sudo yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
   sudo yum install -y docker-ce docker-ce-cli containerd.io git nano
   sudo systemctl enable --now docker
   sudo systemctl status docker
   git clone https://git.jinr.ru/nica_db/emd.git
   cd emd/
   sudo docker build -f Dockerfile.with-build -t nica-ems:buildindocker .
   nano ems.config.yaml   # Set up or check your configuration file
   sudo docker run --rm -it -v ./ems.config.yaml:/app/bin/ems.config.yaml -p 80:8080 nica-ems:buildindocker
   ```

2. **Local installation (for development purpose):**
   ```bash
   sudo yum -y install java-17 git gcc-c++ nano
   git clone https://git.jinr.ru/nica_db/emd.git
   cd emd/
   nano ems.config.yaml   # Set up or check your configuration file
   sh gradlew run
   ```

3. **Local installation but run inside the Docker-container (for development purpose):**
   ```bash
   sudo yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
   sudo yum install -y docker-ce docker-ce-cli containerd.io java-17 git gcc-c++ nano
   sudo systemctl enable --now docker
   sudo systemctl status docker
   git clone https://git.jinr.ru/nica_db/emd.git
   cd emd/
   sh gradlew installDist
   docker build -t nica-ems:current .
   nano ems.config.yaml   # Set up or check your configuration file
   sudo docker run -d --rm --name nica-ems -p 80:8080 -v ./ems.config.yaml:/app/bin/ems.config.yaml nica-ems:current
   ```

### Using the deployed REST API service

#### Supported Methods

1. **Read event records (get event metadata):**
   ```
   GET /event_api/v1/event[?parameter1=value1[&parameter2=value2[...]]]
   ```
   Parameters:
   - Standard parameters:
     - `software_version` (string)
     - `period_number` (short)
     - `run_number` (int)
   - Condition database preselection parameters:
     - `beam_particle` (string)
     - `target_particle` (string)
     - `energy` (float)
   - Pagination:
     - `limit` (int): Limit number of records returned.
     - `offset` (int): Offset for the first entry.
   - Any custom parameters specified in the YAML configuration file.

   Example:
   ```
   http://127.0.0.1:8080/event_api/v1/event?limit=5&software_version=~19._&beam_particle=~A_&track_number=11|
   ```

2. **Create event records in the metadata catalog (Writer or Admin role required):**
   ```
   POST /event_api/v1/event
   ```
   Message body must contain the JSON list of events to be written.

3. **Update event records in the metadata catalog (Writer or Admin role required):**
   ```
   PUT /event_api/v1/event
   ```
   Message body must contain the JSON list of events to be created or updated.

4. **Delete event records from the metadata catalog (Admin role required):**
   ```
   DELETE /event_api/v1/event
   ```
   Message body must contain the JSON list of events (only `reference:` part is required).

5. **Count number of entries in EMS:**
   ```
   GET /event_api/v1/count[?parameter1=value1[&parameter2=value2[...]]]
   ```
   Returns the number of records matching the request in the form `{"count": number}`.

6. **Read software records from dictionary:**
   ```
   GET /event_api/v1/software
   ```

7. **Create software record in dictionary:**
   ```
   POST /event_api/v1/software
   ```
   Example message body:
   ```
   {"software_id": 100, "software_version": "22.11"}
   ```

8. **Read storage records from dictionary:**
   ```
   GET /event_api/v1/storage
   ```

9. **Create storage record in dictionary:**
   ```
   POST /event_api/v1/storage
   ```
   Example message body:
   ```
   {"storage_id": 100, "storage_name": "data1"}
   ```

10. **Get event records as a ROOT file (synchronous):**
    ```
    GET /event_api/v1/eventFile[?parameter1=value1[&parameter2=value2[...]]]
    ```
    File is built and downloaded immediately.

11. **Get event records as a ROOT file reference (asynchronous):**
    ```
    GET /event_api/v1/eventFileRef[?parameter1=value1[&parameter2=value2[...]]]
    ```
    Returns the path to the generated file.

#### JSON Schema for Events
Both GET and POST use the same format for events:
```json
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
```json
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

The GET response also includes some extra data, like this (`limit` is the maximum number of events
that were requested or could be returned, `offset` is offset starting from which events were retrieved,
`count` is equal to actual number of events in `events`:

```json
{
    "events": [ ... ],
    "limit": 50000,
    "offset": 0,
    "count": 100
}
```
Note: when POSTing or PUTing events, `software_version` and `storage_name` must exist in the corresponding 
EMS database tables beforehand. However, the `file_path` entry will be automatically created in 
the `file_` table, if not there yet.

---

## Statistics Collector Script for EMS

### Overview
This script collects statistics on runs stored in the Event Metadata System using data stored in the Condition Database. It organizes collected data into graphs and writes a JSON containing all collected data to the Event Database (table `statistics_`).

### Features
- Collects basic statistics from Event Database: total event count and number of events per period.
- Collects additional data from Condition Database: target and beam particles and beam energy.
- Creates pie charts showing the number of events on various targets for each pair `(beam particle, beam energy)` in a period.
- Writes collected statistics to the Event Database (table `statistics_`).

### Usage
Run the script daily using the following commands:
```bash
uv sync
source .venv/bin/activate
python3 src/main.py [config_path]
```
Or simply:
```bash
uv run src/main.py [config_path]
```

If `config_path` is omitted, the script will look for a file named `ems-collector-config.yaml` in the project root directory.

Example configuration file (`ems-collector-config.yaml`):
```yaml
event_db:
  host: 192.168.65.52
  port: 5000
  db_name: event_db
  user: postgres
  password: example
condition_db:
  host: 192.168.65.53
  port: 5432
  db_name: condition_db
  user: postgres
  password: example
```

### Automation with Systemd

#### Step 1: Install Dependencies
```bash
curl -LsSf https://astral.sh/uv/install.sh | sh
sudo dnf install -y postgresql-devel gcc python3-devel
```

#### Step 2: Create Systemd Service File
What Needs to Be Specified in the File:
1. **`ExecStart`**:
   - This specifies the command to start the script.
   - It should point to the Python executable inside the virtual environment and the main script file.
   - Example: `/opt/emd/ems-stat-collector/.venv/bin/python3 /opt/emd/ems-stat-collector/src/main.py`

2. **`WorkingDirectory`**:
   - This is the working directory where the script will run.
   - It should match the root directory of your project.
   - Example: `/opt/emd/ems-stat-collector`
3. **`User`**:
   - Specifies the user under which the script will run.
   - Example: `username`

If we assume:
- **Project path**: `/opt/emd/ems-stat-collector`
- **Username**: `username`

Create a file `/etc/systemd/system/ems-stat-collector.service` with the following content:
```ini
[Unit]
Description=EMS Stat Collector Script

[Service]
ExecStart=/opt/emd/ems-stat-collector/.venv/bin/python3 /opt/emd/ems-stat-collector/src/main.py
WorkingDirectory=/opt/emd/ems-stat-collector
StandardOutput=append:/var/log/ems-stat-collector.log
StandardError=append:/var/log/ems-stat-collector.err
Restart=on-failure
StartLimitBurst=5
User=username

[Install]
WantedBy=multi-user.target
```

#### Step 3: Create Systemd Timer File
Create a file `/etc/systemd/system/ems-stat-collector.timer` with the following content:
```ini
[Unit]
Description=Run EMS Stat Collector Daily

[Timer]
OnCalendar=daily
Persistent=true

[Install]
WantedBy=timers.target
```

#### Step 4: Enable and Start the Timer
```bash
sudo systemctl daemon-reload
sudo systemctl enable ems-stat-collector.timer
sudo systemctl start ems-stat-collector.timer
```

Now, the script will run automatically every day.

--- 
