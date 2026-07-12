from fastapi import UploadFile

from parts.models import Part, PartPhoto
from parts.repository import PartsRepository
from parts.storage import PhotoStorage


class PartNotFoundError(Exception):
    pass


class PartNumberConflictError(Exception):
    pass


class PartPhotoNotFoundError(Exception):
    pass


class PartsService:
    def __init__(self, repository: PartsRepository, storage: PhotoStorage | None = None) -> None:
        self.repository = repository
        self.storage = storage or PhotoStorage()

    async def list_parts(self, query: str | None = None) -> list[Part]:
        normalized_query = query.strip() if query else None
        if normalized_query == "":
            normalized_query = None
        return await self.repository.list_parts(normalized_query)

    async def get_part(self, part_id: int) -> Part:
        part = await self.repository.get_by_id(part_id)
        if part is None:
            raise PartNotFoundError
        return part

    async def create_part(self, number: str, title: str) -> Part:
        normalized_number = number.strip()
        normalized_title = title.strip()
        if not normalized_number:
            raise ValueError("number is required")
        if not normalized_title:
            raise ValueError("title is required")

        existing = await self.repository.get_by_number(normalized_number)
        if existing is not None:
            raise PartNumberConflictError

        part = Part(number=normalized_number, title=normalized_title)
        return await self.repository.create(part)

    async def update_part(
        self,
        part_id: int,
        number: str | None = None,
        title: str | None = None,
    ) -> Part:
        part = await self.get_part(part_id)

        if number is not None:
            normalized_number = number.strip()
            if not normalized_number:
                raise ValueError("number is required")

            existing = await self.repository.get_by_number(normalized_number, exclude_id=part_id)
            if existing is not None:
                raise PartNumberConflictError
            part.number = normalized_number

        if title is not None:
            normalized_title = title.strip()
            if not normalized_title:
                raise ValueError("title is required")
            part.title = normalized_title

        return await self.repository.update(part)

    async def delete_part(self, part_id: int) -> None:
        part = await self.get_part(part_id)
        for photo in part.photos:
            self.storage.delete_photo(photo.file_path)
        self.storage.delete_part_photos(part_id)
        await self.repository.delete(part)

    async def upload_photo(self, part_id: int, upload: UploadFile) -> PartPhoto:
        await self.get_part(part_id)
        file_path = await self.storage.save_photo(part_id, upload)
        sort_order = await self.repository.next_sort_order(part_id)
        photo = PartPhoto(part_id=part_id, file_path=file_path, sort_order=sort_order)
        return await self.repository.add_photo(photo)

    async def delete_photo(self, part_id: int, photo_id: int) -> None:
        photo = await self.repository.get_photo(part_id, photo_id)
        if photo is None:
            raise PartPhotoNotFoundError

        self.storage.delete_photo(photo.file_path)
        await self.repository.delete_photo(photo)

    async def reorder_photos(self, part_id: int, photo_ids: list[int]) -> list[PartPhoto]:
        part = await self.get_part(part_id)
        existing_ids = {photo.id for photo in part.photos}
        if set(photo_ids) != existing_ids:
            raise ValueError("photo_ids must include all photos for the part")

        return await self.repository.reorder_photos(part_id, photo_ids)
