from datetime import datetime

from sqlalchemy import DateTime, ForeignKey, Integer, String, UniqueConstraint, func
from sqlalchemy.orm import Mapped, mapped_column, relationship

from core.database import Base


class Part(Base):
    __tablename__ = "parts"
    __table_args__ = (UniqueConstraint("number", name="uq_parts_number"),)

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    number: Mapped[str] = mapped_column(String(100), nullable=False)
    title: Mapped[str] = mapped_column(String(255), nullable=False)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        nullable=False,
        server_default=func.now(),
    )
    photos: Mapped[list["PartPhoto"]] = relationship(
        back_populates="part",
        order_by="PartPhoto.sort_order",
        cascade="all, delete-orphan",
    )


class PartPhoto(Base):
    __tablename__ = "part_photos"

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    part_id: Mapped[int] = mapped_column(ForeignKey("parts.id", ondelete="CASCADE"), nullable=False)
    file_path: Mapped[str] = mapped_column(String(500), nullable=False)
    sort_order: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    part: Mapped[Part] = relationship(back_populates="photos")
