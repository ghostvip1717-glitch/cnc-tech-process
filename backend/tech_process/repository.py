from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload

from tech_process.models import Operation, Setup, TechProcess

_SETUPS_WITH_OPERATIONS = selectinload(TechProcess.setups).selectinload(Setup.operations)


class TechProcessRepository:
    def __init__(self, session: AsyncSession) -> None:
        self.session = session

    async def get_by_part_id(self, part_id: int) -> TechProcess | None:
        stmt = (
            select(TechProcess)
            .options(_SETUPS_WITH_OPERATIONS)
            .where(TechProcess.part_id == part_id)
        )
        result = await self.session.execute(stmt)
        return result.scalar_one_or_none()

    async def get_by_id(self, tech_process_id: int) -> TechProcess | None:
        stmt = (
            select(TechProcess)
            .options(_SETUPS_WITH_OPERATIONS)
            .where(TechProcess.id == tech_process_id)
        )
        result = await self.session.execute(stmt)
        return result.scalar_one_or_none()

    async def create(self, tech_process: TechProcess) -> TechProcess:
        self.session.add(tech_process)
        await self.session.commit()
        return await self.get_by_part_id(tech_process.part_id)  # type: ignore[return-value]

    async def get_setup(self, tech_process_id: int, setup_id: int) -> Setup | None:
        stmt = (
            select(Setup)
            .options(selectinload(Setup.operations))
            .where(Setup.id == setup_id, Setup.tech_process_id == tech_process_id)
        )
        result = await self.session.execute(stmt)
        return result.scalar_one_or_none()

    async def add_setup(self, setup: Setup) -> Setup:
        self.session.add(setup)
        await self.session.commit()
        await self.session.refresh(setup)
        return setup

    async def update_setup(self, setup: Setup) -> Setup:
        await self.session.commit()
        await self.session.refresh(setup)
        return setup

    async def delete_setup(self, setup: Setup) -> None:
        await self.session.delete(setup)
        await self.session.commit()

    async def next_setup_order(self, tech_process_id: int) -> int:
        tech_process = await self.get_by_id(tech_process_id)
        if tech_process is None or not tech_process.setups:
            return 0
        return max(setup.order for setup in tech_process.setups) + 1

    async def get_operation(self, operation_id: int) -> Operation | None:
        stmt = (
            select(Operation)
            .options(selectinload(Operation.setup).selectinload(Setup.tech_process))
            .where(Operation.id == operation_id)
        )
        result = await self.session.execute(stmt)
        return result.scalar_one_or_none()

    async def get_operation_in_setup(self, setup_id: int, operation_id: int) -> Operation | None:
        stmt = select(Operation).where(Operation.id == operation_id, Operation.setup_id == setup_id)
        result = await self.session.execute(stmt)
        return result.scalar_one_or_none()

    async def add_operation(self, operation: Operation) -> Operation:
        self.session.add(operation)
        await self.session.commit()
        await self.session.refresh(operation)
        return operation

    async def update_operation(self, operation: Operation) -> Operation:
        await self.session.commit()
        await self.session.refresh(operation)
        return operation

    async def delete_operation(self, operation: Operation) -> None:
        await self.session.delete(operation)
        await self.session.commit()

    async def next_operation_order(self, setup_id: int) -> int:
        stmt = select(Operation).where(Operation.setup_id == setup_id)
        result = await self.session.execute(stmt)
        operations = list(result.scalars().all())
        if not operations:
            return 0
        return max(operation.order for operation in operations) + 1

    async def reorder_operations(self, setup_id: int, operation_ids: list[int]) -> list[Operation]:
        stmt = select(Operation).where(Operation.setup_id == setup_id)
        result = await self.session.execute(stmt)
        operations = list(result.scalars().all())
        operations_by_id = {operation.id: operation for operation in operations}

        for index, operation_id in enumerate(operation_ids):
            operation = operations_by_id.get(operation_id)
            if operation is not None:
                operation.order = index

        await self.session.commit()

        refreshed = await self.session.execute(
            select(Operation).where(Operation.setup_id == setup_id).order_by(Operation.order.asc()),
        )
        return list(refreshed.scalars().all())
