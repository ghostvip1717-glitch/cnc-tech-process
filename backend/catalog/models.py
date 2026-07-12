import enum

from sqlalchemy import Enum, String, UniqueConstraint
from sqlalchemy.orm import Mapped, mapped_column

from core.database import Base


class CatalogItemType(str, enum.Enum):
    tool = "tool"
    plate = "plate"
    jaw = "jaw"


class CatalogItem(Base):
    __tablename__ = "catalog_items"
    __table_args__ = (UniqueConstraint("type", "name", name="uq_catalog_items_type_name"),)

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    type: Mapped[CatalogItemType] = mapped_column(
        Enum(CatalogItemType, name="catalog_item_type", native_enum=False, length=16),
        nullable=False,
    )
    name: Mapped[str] = mapped_column(String(255), nullable=False)
    note: Mapped[str | None] = mapped_column(String(1000), nullable=True)
