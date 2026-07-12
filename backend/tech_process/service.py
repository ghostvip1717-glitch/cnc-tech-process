from catalog.models import CatalogItemType
from catalog.repository import CatalogRepository
from parts.repository import PartsRepository
from tech_process.models import Setup, TechProcess
from tech_process.repository import TechProcessRepository


class TechProcessNotFoundError(Exception):
    pass


class SetupNotFoundError(Exception):
    pass


class PartNotFoundError(Exception):
    pass


class InvalidJawError(Exception):
    pass


class TechProcessAlreadyExistsError(Exception):
    pass


class TechProcessService:
    def __init__(
        self,
        repository: TechProcessRepository,
        parts_repository: PartsRepository,
        catalog_repository: CatalogRepository,
    ) -> None:
        self.repository = repository
        self.parts_repository = parts_repository
        self.catalog_repository = catalog_repository

    async def _ensure_part_exists(self, part_id: int) -> None:
        part = await self.parts_repository.get_by_id(part_id)
        if part is None:
            raise PartNotFoundError

    async def _validate_jaw_id(self, jaw_id: int) -> None:
        item = await self.catalog_repository.get_by_id(jaw_id)
        if item is None or item.type != CatalogItemType.jaw:
            raise InvalidJawError

    async def get_tech_process(self, part_id: int) -> TechProcess:
        await self._ensure_part_exists(part_id)
        tech_process = await self.repository.get_by_part_id(part_id)
        if tech_process is None:
            raise TechProcessNotFoundError
        return tech_process

    async def create_tech_process(self, part_id: int) -> TechProcess:
        await self._ensure_part_exists(part_id)
        existing = await self.repository.get_by_part_id(part_id)
        if existing is not None:
            raise TechProcessAlreadyExistsError

        tech_process = TechProcess(part_id=part_id)
        return await self.repository.create(tech_process)

    async def get_or_create_tech_process(self, part_id: int) -> TechProcess:
        await self._ensure_part_exists(part_id)
        tech_process = await self.repository.get_by_part_id(part_id)
        if tech_process is not None:
            return tech_process
        return await self.repository.create(TechProcess(part_id=part_id))

    async def add_setup(self, part_id: int, jaw_id: int) -> Setup:
        await self._validate_jaw_id(jaw_id)
        tech_process = await self.get_or_create_tech_process(part_id)
        order = await self.repository.next_setup_order(tech_process.id)
        setup = Setup(tech_process_id=tech_process.id, order=order, jaw_id=jaw_id)
        return await self.repository.add_setup(setup)

    async def update_setup(self, part_id: int, setup_id: int, jaw_id: int) -> Setup:
        await self._validate_jaw_id(jaw_id)
        tech_process = await self.get_tech_process(part_id)
        setup = await self.repository.get_setup(tech_process.id, setup_id)
        if setup is None:
            raise SetupNotFoundError

        setup.jaw_id = jaw_id
        return await self.repository.update_setup(setup)

    async def delete_setup(self, part_id: int, setup_id: int) -> None:
        tech_process = await self.get_tech_process(part_id)
        setup = await self.repository.get_setup(tech_process.id, setup_id)
        if setup is None:
            raise SetupNotFoundError
        await self.repository.delete_setup(setup)
