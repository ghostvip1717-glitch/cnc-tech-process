from catalog.models import CatalogItem, CatalogItemType
from catalog.repository import CatalogRepository


class CatalogNameConflictError(Exception):
    pass


class CatalogItemNotFoundError(Exception):
    pass


class CatalogService:
    def __init__(self, repository: CatalogRepository) -> None:
        self.repository = repository

    async def list_items(
        self,
        item_type: CatalogItemType | None = None,
        query: str | None = None,
    ) -> list[CatalogItem]:
        normalized_query = query.strip() if query else None
        if normalized_query == "":
            normalized_query = None
        return await self.repository.list_items(item_type, normalized_query)

    async def get_item(self, item_id: int) -> CatalogItem:
        item = await self.repository.get_by_id(item_id)
        if item is None:
            raise CatalogItemNotFoundError
        return item

    async def create_item(
        self,
        item_type: CatalogItemType,
        name: str,
        note: str | None = None,
    ) -> CatalogItem:
        normalized_name = name.strip()
        if not normalized_name:
            raise ValueError("name is required")

        existing = await self.repository.get_by_type_and_name(item_type, normalized_name)
        if existing is not None:
            raise CatalogNameConflictError

        item = CatalogItem(type=item_type, name=normalized_name, note=note)
        return await self.repository.create(item)

    async def update_item(
        self,
        item_id: int,
        name: str | None = None,
        note: str | None = None,
    ) -> CatalogItem:
        item = await self.get_item(item_id)

        if name is not None:
            normalized_name = name.strip()
            if not normalized_name:
                raise ValueError("name is required")

            existing = await self.repository.get_by_type_and_name(
                item.type,
                normalized_name,
                exclude_id=item_id,
            )
            if existing is not None:
                raise CatalogNameConflictError
            item.name = normalized_name

        if note is not None:
            item.note = note

        return await self.repository.update(item)

    async def delete_item(self, item_id: int) -> None:
        item = await self.get_item(item_id)
        await self.repository.delete(item)
