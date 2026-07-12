from sqlalchemy import or_, select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload

from parts.models import Part, PartPhoto


class PartsRepository:
    def __init__(self, session: AsyncSession) -> None:
        self.session = session

    async def list_parts(self, query: str | None = None) -> list[Part]:
        stmt = select(Part).options(selectinload(Part.photos)).order_by(Part.created_at.desc())

        if query:
            pattern = f"%{query}%"
            stmt = stmt.where(or_(Part.number.ilike(pattern), Part.title.ilike(pattern)))

        result = await self.session.execute(stmt)
        return list(result.scalars().unique().all())

    async def get_by_id(self, part_id: int) -> Part | None:
        stmt = select(Part).options(selectinload(Part.photos)).where(Part.id == part_id)
        result = await self.session.execute(stmt)
        return result.scalar_one_or_none()

    async def get_by_number(self, number: str, exclude_id: int | None = None) -> Part | None:
        stmt = select(Part).where(Part.number == number)
        if exclude_id is not None:
            stmt = stmt.where(Part.id != exclude_id)

        result = await self.session.execute(stmt)
        return result.scalar_one_or_none()

    async def create(self, part: Part) -> Part:
        self.session.add(part)
        await self.session.commit()
        return await self.get_by_id(part.id)  # type: ignore[return-value]

    async def update(self, part: Part) -> Part:
        await self.session.commit()
        refreshed = await self.get_by_id(part.id)
        return refreshed  # type: ignore[return-value]

    async def delete(self, part: Part) -> None:
        await self.session.delete(part)
        await self.session.commit()

    async def get_photo(self, part_id: int, photo_id: int) -> PartPhoto | None:
        stmt = select(PartPhoto).where(PartPhoto.id == photo_id, PartPhoto.part_id == part_id)
        result = await self.session.execute(stmt)
        return result.scalar_one_or_none()

    async def add_photo(self, photo: PartPhoto) -> PartPhoto:
        self.session.add(photo)
        await self.session.commit()
        await self.session.refresh(photo)
        return photo

    async def delete_photo(self, photo: PartPhoto) -> None:
        await self.session.delete(photo)
        await self.session.commit()

    async def reorder_photos(self, part_id: int, photo_ids: list[int]) -> list[PartPhoto]:
        part = await self.get_by_id(part_id)
        if part is None:
            return []

        photos_by_id = {photo.id: photo for photo in part.photos}
        for index, photo_id in enumerate(photo_ids):
            photo = photos_by_id.get(photo_id)
            if photo is not None:
                photo.sort_order = index

        await self.session.commit()
        refreshed = await self.get_by_id(part_id)
        return refreshed.photos if refreshed else []

    async def next_sort_order(self, part_id: int) -> int:
        part = await self.get_by_id(part_id)
        if part is None or not part.photos:
            return 0
        return max(photo.sort_order for photo in part.photos) + 1
