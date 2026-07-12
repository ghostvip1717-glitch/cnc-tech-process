from catalog.models import CatalogItemType
from catalog.repository import CatalogRepository
from parts.repository import PartsRepository
from tech_process.repository import TechProcessRepository


class PartNotFoundError(Exception):
    pass


class RequiredCatalogItem:
    def __init__(self, item_id: int, item_type: CatalogItemType, name: str) -> None:
        self.id = item_id
        self.type = item_type
        self.name = name


class RequiredItemsResult:
    def __init__(
        self,
        tools: list[RequiredCatalogItem],
        plates: list[RequiredCatalogItem],
        jaws: list[RequiredCatalogItem],
    ) -> None:
        self.tools = tools
        self.plates = plates
        self.jaws = jaws


class AssemblyService:
    def __init__(
        self,
        parts_repository: PartsRepository,
        tech_process_repository: TechProcessRepository,
        catalog_repository: CatalogRepository,
    ) -> None:
        self.parts_repository = parts_repository
        self.tech_process_repository = tech_process_repository
        self.catalog_repository = catalog_repository

    async def get_required_items(self, part_id: int) -> RequiredItemsResult:
        part = await self.parts_repository.get_by_id(part_id)
        if part is None:
            raise PartNotFoundError

        tech_process = await self.tech_process_repository.get_by_part_id(part_id)
        if tech_process is None:
            return RequiredItemsResult([], [], [])

        tool_ids: set[int] = set()
        plate_ids: set[int] = set()
        jaw_ids: set[int] = set()

        for setup in tech_process.setups:
            jaw_ids.add(setup.jaw_id)
            for operation in setup.operations:
                tool_ids.add(operation.tool_id)
                plate_ids.add(operation.plate_id)

        tools = await self._resolve_items(tool_ids, CatalogItemType.tool)
        plates = await self._resolve_items(plate_ids, CatalogItemType.plate)
        jaws = await self._resolve_items(jaw_ids, CatalogItemType.jaw)

        return RequiredItemsResult(tools, plates, jaws)

    async def _resolve_items(
        self,
        item_ids: set[int],
        expected_type: CatalogItemType,
    ) -> list[RequiredCatalogItem]:
        items: list[RequiredCatalogItem] = []
        for item_id in sorted(item_ids):
            catalog_item = await self.catalog_repository.get_by_id(item_id)
            if catalog_item is None or catalog_item.type != expected_type:
                continue
            items.append(
                RequiredCatalogItem(
                    item_id=catalog_item.id,
                    item_type=catalog_item.type,
                    name=catalog_item.name,
                ),
            )
        return items
