from sqlalchemy.orm import Mapped
from sqlalchemy.orm import mapped_column
from orm.base import Base


class Run(Base):
    __tablename__ = "run_"
    period_number: Mapped[int] = mapped_column(primary_key=True)
    run_number: Mapped[int] = mapped_column(primary_key=True)
    beam_particle: Mapped[str]
    target_particle: Mapped[str]
    energy: Mapped[float]
