from sqlalchemy import insert
from yaml import safe_load
from pprint import pprint
from pathlib import Path
from sys import argv
import json

from condition_db_reader import ConditionDBReader
from ems_stat_collector import EMSStatCollector
from conn_parameters import ConnParameters
from orm.ems import Statistics


DEFAULT_CONFIG_PATH = "../ems-stat-collector-config.yaml"


def get_connection_urls(path_to_config):
    config_parsed = None
    with open(path_to_config, "r") as input_file:
        config_raw = input_file.read()
        config_parsed = safe_load(config_raw)

    connection_urls = {}
    records = ["event_db", "condition_db"]
    for record in records:
        parameters_dict = config_parsed.get(record, None)
        if parameters_dict is None:
            raise ValueError(f"Record \"{record}\" is missing from the config")
        try:
            p = ConnParameters.from_dict(parameters_dict)
        except ValueError as err:
            raise ValueError(f"Record \"{record}\": {err}")

        connection_urls[record] = f"postgresql://{p.user}:{p.password}@{p.host}:{p.port}/{p.db_name}"
    
    return connection_urls


def get_path_to_config():
    if len(argv) > 1:
        config_path = Path(argv[1])
    else:
        source_path = Path(__file__)
        config_path = (
            source_path.absolute()
            .parent
            .joinpath(DEFAULT_CONFIG_PATH)
        )
    return config_path


def convert_stats_to_ems_format(stats):
    stats_ems = {"totalRecords": stats["totalRecords"], "periodStats": []}
    for period_num, period_data in stats["periodStats"].items():
        sw_stats = []
        for sw_name, sw_data in period_data["softwareStats"].items():
            # Every (beam, energy) has its graph
            graphs = []
            for (beam, energy), targets_dict in sw_data.items():
                graph = {
                    "title1": f"Beam {beam} ( E = {energy} GeV/n )",
                    "title2": "",   # Updated later
                    "slices": []
                }

                totForSWandPeriod = 0
                for target, count in targets_dict.items():
                    totForSWandPeriod += count
                    graph["slices"].append({
                        "name": target or "no target",
                        "value": count
                    })

                graph["title2"] = f"Total: {totForSWandPeriod} events"  # TODO: MEvents, etc.
                graphs.append(graph)

            sw_stats.append({
                "swVer": sw_name,
                "graphs": graphs
            })

        new_per = {
            "periodNumber": period_num,
            "periodRecords": period_data["periodRecords"],
            "softwareStats": sw_stats
        }
        stats_ems["periodStats"].append(new_per)
    return stats_ems


if __name__ == "__main__":
    urls = get_connection_urls(get_path_to_config())
    condDBReader = ConditionDBReader(urls["condition_db"])
    # print(condDBReader.get_bte(8, 8000))

    emsReader = EMSStatCollector(urls["event_db"], condDBReader)
    stats = emsReader.collect()
    pprint(stats)

    # Same data but using exactly the required EMS schema
    stats_ems_format = convert_stats_to_ems_format(stats)
    pprint(stats_ems_format)

    emsReader.session_ems.execute(
        insert(Statistics), [{"json_stats": json.dumps(stats_ems_format)}]
    )
    emsReader.session_ems.commit()


