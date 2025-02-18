from sqlalchemy.orm import Mapped
from sqlalchemy.orm import mapped_column
from sqlalchemy.orm import relationship
from sqlalchemy import ForeignKey, DateTime, func
import datetime

from orm.base import Base


class Software(Base):
    __tablename__ = "software_"
    software_id: Mapped[int] = mapped_column(primary_key=True)
    software_version: Mapped[str] = mapped_column(unique=True)


class Storage(Base):
    __tablename__ = "storage_"
    storage_id: Mapped[int] = mapped_column(primary_key=True)
    storage_name: Mapped[str] = mapped_column(unique=True)


class File(Base):
    __tablename__ = "file_"
    file_guid: Mapped[int] = mapped_column(primary_key=True)
    file_path: Mapped[str] = mapped_column(nullable=False)
    storage_id: Mapped[int] = mapped_column(ForeignKey("storage_.storage_id"))
    storage: Mapped[Storage] = relationship()


class Event(Base):
    __tablename__ = "event"
    file_guid: Mapped[int] = mapped_column(ForeignKey("file_.file_guid"), primary_key=True)
    file: Mapped[File] = relationship()
    event_number: Mapped[int] = mapped_column(primary_key=True)

    software_id: Mapped[int] = mapped_column(ForeignKey("software_.software_id"))
    software: Mapped[Software] = relationship()
    period_number: Mapped[int] = mapped_column(nullable=False)
    run_number: Mapped[int] = mapped_column(nullable=False)

    track_number: Mapped[int] = mapped_column(default=-1, nullable=False)


class Statistics(Base):
    __tablename__ = "statistics"
    id: Mapped[int] = mapped_column(primary_key=True)
    json_stats: Mapped[str]
    time_written: Mapped[datetime.datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now()
    )

