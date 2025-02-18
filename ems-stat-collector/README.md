
## Statistics collector script for Event Metadata System

This script collects statistics on runs stored in the Event Metadata System using data stored in the Condition Database. 
The connection parameters can be read from an EMS configuration file or a standalone file (see an example in the project directory).

Current edition of the script:
- Collects basic statistics from Event Database: total event count and number of events per period;
- Collects additional data from Condition Database: target and beam particles and beam energy;
- Organizes collected data into graphs: for each pair `(beam particle, beam energy)` in a period a pie chart showing number of events on various targets is created;
- A JSON containing all collected data is written to the Event Database (table `statistics`).

The collected statistics should be displayed by EMS Web UI (and also available at `/event_api/v1/statistics` URL).

### Usage

```
uv sync
source .venv/bin/activate
python3 src/main.py [config_path]
```

or just 

```
uv run src/main.py [config_path]
```

If `config_path` is omitted, the script will look for a file named `ems-collector-config.yaml` in the project root 
directory. Its example content:

```
event_db:
  host: 192.168.65.52
  port: 5000
  db_name: event_db
  user: postgres
  password: example

condition_db:
  host: nc13.jinr.ru
  port: 5432
  db_name: bmn_db
  user: db_reader
  password: reader_pass
```
