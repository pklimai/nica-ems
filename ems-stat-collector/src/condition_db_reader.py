from functools import lru_cache

from sqlalchemy import create_engine, select
from sqlalchemy.orm import Session

from bte import BTE
from orm.condition import Run


class ConditionDBReader:
    def __init__(self, url):
        self.engine_condition = create_engine(url)
        self.engine_condition.connect()
        self.session_condition = Session(self.engine_condition)

    @lru_cache(100_000)
    def get_bte(self, period, run):
        stmt = select(Run).where(Run.period_number.in_([period]) & Run.run_number.in_([run]))
        run = self.session_condition.scalar(stmt)
        if run is None:
            return None
        else:
            return BTE(run.beam_particle, run.target_particle, run.energy)
