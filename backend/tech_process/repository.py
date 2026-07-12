from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload

from tech_process.models import Setup, TechProcess


class TechProcessRepository:
    def __init__(self, session: AsyncSession) -> None:
        self.session = session

    async def get_by_part_id(self, part_id: int) -> TechProcess | None:
        stmt = (
            select(TechProcess)
            .options(selectinload(TechProcess.setups))
            .where(TechProcess.part_id == part_id)
        )
        result = await self.session.execute(stmt)
        return result.scalar_one_or_none()

    async def get_by_id(self, tech_process_id: int) -> TechProcess | None:
        stmt = (
            select(TechProcess)
            .options(selectinload(TechProcess.setups))
            .where(TechProcess.id == tech_process_id)
        )
        result = await self.session.execute(stmt)
        return result.scalar_one_or_none()

    async def create(self, tech_process: TechProcess) -> TechProcess:
        self.session.add(tech_process)
        await self.session.commit()
        return await self.get_by_part_id(tech_process.part_id)  # type: ignore[return-value]

    async def get_setup(self, tech_process_id: int, setup_id: int) -> Setup | None:
        stmt = select(Setup).where(
            Setup.id == setup_id,
            Setup.tech_process_id == tech_process_id,
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
