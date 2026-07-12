from sqlalchemy import ForeignKey, Integer, String, UniqueConstraint
from sqlalchemy.orm import Mapped, mapped_column, relationship

from core.database import Base


class TechProcess(Base):
    __tablename__ = "tech_processes"
    __table_args__ = (UniqueConstraint("part_id", name="uq_tech_processes_part_id"),)

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    part_id: Mapped[int] = mapped_column(ForeignKey("parts.id", ondelete="CASCADE"), nullable=False)
    setups: Mapped[list["Setup"]] = relationship(
        back_populates="tech_process",
        order_by="Setup.order",
        cascade="all, delete-orphan",
    )


class Setup(Base):
    __tablename__ = "setups"

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    tech_process_id: Mapped[int] = mapped_column(
        ForeignKey("tech_processes.id", ondelete="CASCADE"),
        nullable=False,
    )
    order: Mapped[int] = mapped_column("order", Integer, nullable=False)
    jaw_id: Mapped[int] = mapped_column(ForeignKey("catalog_items.id"), nullable=False)
    tech_process: Mapped[TechProcess] = relationship(back_populates="setups")
    operations: Mapped[list["Operation"]] = relationship(
        back_populates="setup",
        order_by="Operation.order",
        cascade="all, delete-orphan",
    )


class Operation(Base):
    __tablename__ = "operations"

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    setup_id: Mapped[int] = mapped_column(ForeignKey("setups.id", ondelete="CASCADE"), nullable=False)
    order: Mapped[int] = mapped_column("order", Integer, nullable=False)
    op_number: Mapped[str] = mapped_column(String(16), nullable=False)
    title: Mapped[str] = mapped_column(String(500), nullable=False)
    tool_id: Mapped[int] = mapped_column(ForeignKey("catalog_items.id"), nullable=False)
    plate_id: Mapped[int] = mapped_column(ForeignKey("catalog_items.id"), nullable=False)
    comment: Mapped[str | None] = mapped_column(String(1000), nullable=True)
    setup: Mapped[Setup] = relationship(back_populates="operations")
