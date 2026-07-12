from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from catalog.models import CatalogItem, CatalogItemType


class CatalogRepository:
    def __init__(self, session: AsyncSession) -> None:
        self.session = session

    async def list_items(
        self,
        item_type: CatalogItemType | None = None,
        query: str | None = None,
    ) -> list[CatalogItem]:
        stmt = select(CatalogItem).order_by(CatalogItem.name.asc())

        if item_type is not None:
            stmt = stmt.where(CatalogItem.type == item_type)

        if query:
            stmt = stmt.where(CatalogItem.name.ilike(f"%{query}%"))

        result = await self.session.execute(stmt)
        return list(result.scalars().all())

    async def get_by_id(self, item_id: int) -> CatalogItem | None:
        return await self.session.get(CatalogItem, item_id)

    async def get_by_type_and_name(
        self,
        item_type: CatalogItemType,
        name: str,
        exclude_id: int | None = None,
    ) -> CatalogItem | None:
        stmt = select(CatalogItem).where(
            CatalogItem.type == item_type,
            CatalogItem.name == name,
        )
        if exclude_id is not None:
            stmt = stmt.where(CatalogItem.id != exclude_id)

        result = await self.session.execute(stmt)
        return result.scalar_one_or_none()

    async def create(self, item: CatalogItem) -> CatalogItem:
        self.session.add(item)
        await self.session.commit()
        await self.session.refresh(item)
        return item

    async def update(self, item: CatalogItem) -> CatalogItem:
        await self.session.commit()
        await self.session.refresh(item)
        return item

    async def delete(self, item: CatalogItem) -> None:
        await self.session.delete(item)
        await self.session.commit()
