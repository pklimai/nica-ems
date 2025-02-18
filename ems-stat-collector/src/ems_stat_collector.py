from sqlalchemy import create_engine, func
from sqlalchemy import select, distinct
from sqlalchemy.orm import Session

from orm.ems import Event, Software

class EMSStatCollector:
    def __init__(self, url, cond_db_reader):
        self.cond_db_reader = cond_db_reader
        self.engine_ems = create_engine(url)
        self.engine_ems.connect()
        self.session_ems = Session(self.engine_ems)

    def collect(self):
        stats = {"totalRecords": self.session_ems.query(func.count(Event.period_number)).scalar()}
        period_stats = {}
        
        stmt = (
            select(distinct(Event.period_number), Software.software_version, func.count(Event.event_number))
            .select_from(Event)
            .join(Software)
            .group_by(Event.period_number, Software.software_version)
            .order_by(Event.period_number)
        )
        raw_event_stats = self.session_ems.execute(stmt)

        for period_number, software_version, records_number in raw_event_stats:
            if period_number not in period_stats:
                period_stats[period_number] = {"periodRecords": 0, "softwareStats": {}}
            
            period_stats[period_number]["periodRecords"] += records_number
            period_stats[period_number]["softwareStats"][software_version] = {}
        
        for period_number, period_data in period_stats.items():
            stmt = (
                select(distinct(Event.run_number), Software.software_version, func.count(Event.event_number))
                .select_from(Event)
                .join(Software)
                .where(Event.period_number.in_([period_number]))
                .group_by(Event.run_number, Software.software_version)
            )
            run_list = self.session_ems.execute(stmt)
            for run_number, software_version, event_count in run_list:
                bte = self.cond_db_reader.get_bte(period_number, run_number)
                if bte is None:
                    continue

                b = bte.beam
                t = bte.target
                e = bte.energy

                if (b, e) not in period_data["softwareStats"][software_version]:
                    period_data["softwareStats"][software_version][(b, e)] = {}

                if t not in period_data["softwareStats"][software_version][(b, e)]:
                    period_data["softwareStats"][software_version][(b, e)][t] = 0

                period_data["softwareStats"][software_version][(b, e)][t] += event_count

        stats["periodStats"] = period_stats
        return stats
