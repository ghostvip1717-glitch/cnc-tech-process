from catalog.models import CatalogItemType
from catalog.repository import CatalogRepository
from parts.repository import PartsRepository
from tech_process.models import Operation, Setup, TechProcess
from tech_process.repository import TechProcessRepository


class TechProcessNotFoundError(Exception):
    pass


class SetupNotFoundError(Exception):
    pass


class OperationNotFoundError(Exception):
    pass


class PartNotFoundError(Exception):
    pass


class InvalidJawError(Exception):
    pass


class InvalidToolError(Exception):
    pass


class InvalidPlateError(Exception):
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

    async def _validate_tool_id(self, tool_id: int) -> None:
        item = await self.catalog_repository.get_by_id(tool_id)
        if item is None or item.type != CatalogItemType.tool:
            raise InvalidToolError

    async def _validate_plate_id(self, plate_id: int) -> None:
        item = await self.catalog_repository.get_by_id(plate_id)
        if item is None or item.type != CatalogItemType.plate:
            raise InvalidPlateError

    async def _get_setup_for_part(self, part_id: int, setup_id: int) -> Setup:
        tech_process = await self.get_tech_process(part_id)
        setup = await self.repository.get_setup(tech_process.id, setup_id)
        if setup is None:
            raise SetupNotFoundError
        return setup

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
        setup = await self._get_setup_for_part(part_id, setup_id)
        setup.jaw_id = jaw_id
        return await self.repository.update_setup(setup)

    async def delete_setup(self, part_id: int, setup_id: int) -> None:
        setup = await self._get_setup_for_part(part_id, setup_id)
        await self.repository.delete_setup(setup)

    async def add_operation(
        self,
        part_id: int,
        setup_id: int,
        op_number: str,
        title: str,
        tool_id: int,
        plate_id: int,
        comment: str | None = None,
    ) -> Operation:
        await self._validate_tool_id(tool_id)
        await self._validate_plate_id(plate_id)
        setup = await self._get_setup_for_part(part_id, setup_id)

        normalized_op_number = op_number.strip()
        normalized_title = title.strip()
        if not normalized_op_number:
            raise ValueError("op_number is required")
        if not normalized_title:
            raise ValueError("title is required")

        order = await self.repository.next_operation_order(setup.id)
        operation = Operation(
            setup_id=setup.id,
            order=order,
            op_number=normalized_op_number,
            title=normalized_title,
            tool_id=tool_id,
            plate_id=plate_id,
            comment=comment,
        )
        return await self.repository.add_operation(operation)

    async def update_operation(
        self,
        part_id: int,
        operation_id: int,
        op_number: str | None = None,
        title: str | None = None,
        tool_id: int | None = None,
        plate_id: int | None = None,
        comment: str | None = None,
    ) -> Operation:
        operation = await self._get_operation_for_part(part_id, operation_id)

        if tool_id is not None:
            await self._validate_tool_id(tool_id)
            operation.tool_id = tool_id

        if plate_id is not None:
            await self._validate_plate_id(plate_id)
            operation.plate_id = plate_id

        if op_number is not None:
            normalized_op_number = op_number.strip()
            if not normalized_op_number:
                raise ValueError("op_number is required")
            operation.op_number = normalized_op_number

        if title is not None:
            normalized_title = title.strip()
            if not normalized_title:
                raise ValueError("title is required")
            operation.title = normalized_title

        if comment is not None:
            operation.comment = comment

        return await self.repository.update_operation(operation)

    async def delete_operation(self, part_id: int, operation_id: int) -> None:
        operation = await self._get_operation_for_part(part_id, operation_id)
        await self.repository.delete_operation(operation)

    async def reorder_operations(
        self,
        part_id: int,
        setup_id: int,
        operation_ids: list[int],
    ) -> list[Operation]:
        setup = await self._get_setup_for_part(part_id, setup_id)
        existing_ids = {operation.id for operation in setup.operations}
        if set(operation_ids) != existing_ids:
            raise ValueError("operation_ids must include all operations for the setup")

        return await self.repository.reorder_operations(setup.id, operation_ids)

    async def _get_operation_for_part(self, part_id: int, operation_id: int) -> Operation:
        await self._ensure_part_exists(part_id)
        operation = await self.repository.get_operation(operation_id)
        if operation is None:
            raise OperationNotFoundError

        tech_process = await self.repository.get_by_part_id(part_id)
        if tech_process is None or operation.setup.tech_process_id != tech_process.id:
            raise OperationNotFoundError

        return operation
