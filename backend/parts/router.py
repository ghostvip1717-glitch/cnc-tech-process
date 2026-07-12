from typing import Annotated

from fastapi import APIRouter, Depends, File, HTTPException, Query, UploadFile, status
from pydantic import BaseModel, Field
from sqlalchemy.ext.asyncio import AsyncSession

from core.database import get_db
from parts.repository import PartsRepository
from parts.service import (
    PartNotFoundError,
    PartNumberConflictError,
    PartPhotoNotFoundError,
    PartsService,
)
from parts.storage import PhotoStorage

router = APIRouter(prefix="/parts", tags=["parts"])


class PartPhotoResponse(BaseModel):
    id: int
    part_id: int
    file_path: str
    url: str
    sort_order: int


class PartResponse(BaseModel):
    id: int
    number: str
    title: str
    created_at: str
    photos: list[PartPhotoResponse]


class PartCreateRequest(BaseModel):
    number: str = Field(min_length=1, max_length=100)
    title: str = Field(min_length=1, max_length=255)


class PartUpdateRequest(BaseModel):
    number: str | None = Field(default=None, min_length=1, max_length=100)
    title: str | None = Field(default=None, min_length=1, max_length=255)


class PhotoReorderRequest(BaseModel):
    photo_ids: list[int] = Field(min_length=1)


def get_parts_service(
    session: Annotated[AsyncSession, Depends(get_db)],
) -> PartsService:
    return PartsService(PartsRepository(session), PhotoStorage())


def _part_to_response(part, storage: PhotoStorage) -> PartResponse:
    return PartResponse(
        id=part.id,
        number=part.number,
        title=part.title,
        created_at=part.created_at.isoformat(),
        photos=[
            PartPhotoResponse(
                id=photo.id,
                part_id=photo.part_id,
                file_path=photo.file_path,
                url=storage.public_url(photo.file_path),
                sort_order=photo.sort_order,
            )
            for photo in part.photos
        ],
    )


@router.get("", response_model=list[PartResponse])
async def list_parts(
    service: Annotated[PartsService, Depends(get_parts_service)],
    q: Annotated[str | None, Query()] = None,
) -> list[PartResponse]:
    parts = await service.list_parts(q)
    return [_part_to_response(part, service.storage) for part in parts]


@router.post("", response_model=PartResponse, status_code=status.HTTP_201_CREATED)
async def create_part(
    payload: PartCreateRequest,
    service: Annotated[PartsService, Depends(get_parts_service)],
) -> PartResponse:
    try:
        part = await service.create_part(payload.number, payload.title)
    except PartNumberConflictError as exc:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Part with this number already exists",
        ) from exc
    except ValueError as exc:
        raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_ENTITY, detail=str(exc)) from exc

    return _part_to_response(part, service.storage)


@router.get("/{part_id}", response_model=PartResponse)
async def get_part(
    part_id: int,
    service: Annotated[PartsService, Depends(get_parts_service)],
) -> PartResponse:
    try:
        part = await service.get_part(part_id)
    except PartNotFoundError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Part not found") from exc

    return _part_to_response(part, service.storage)


@router.patch("/{part_id}", response_model=PartResponse)
async def update_part(
    part_id: int,
    payload: PartUpdateRequest,
    service: Annotated[PartsService, Depends(get_parts_service)],
) -> PartResponse:
    if payload.number is None and payload.title is None:
        raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_ENTITY, detail="No fields to update")

    try:
        part = await service.update_part(part_id, payload.number, payload.title)
    except PartNotFoundError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Part not found") from exc
    except PartNumberConflictError as exc:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Part with this number already exists",
        ) from exc
    except ValueError as exc:
        raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_ENTITY, detail=str(exc)) from exc

    return _part_to_response(part, service.storage)


@router.delete("/{part_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_part(
    part_id: int,
    service: Annotated[PartsService, Depends(get_parts_service)],
) -> None:
    try:
        await service.delete_part(part_id)
    except PartNotFoundError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Part not found") from exc


@router.post("/{part_id}/photos", response_model=PartPhotoResponse, status_code=status.HTTP_201_CREATED)
async def upload_part_photo(
    part_id: int,
    service: Annotated[PartsService, Depends(get_parts_service)],
    file: Annotated[UploadFile, File()],
) -> PartPhotoResponse:
    try:
        photo = await service.upload_photo(part_id, file)
    except PartNotFoundError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Part not found") from exc
    except ValueError as exc:
        raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_ENTITY, detail=str(exc)) from exc

    return PartPhotoResponse(
        id=photo.id,
        part_id=photo.part_id,
        file_path=photo.file_path,
        url=service.storage.public_url(photo.file_path),
        sort_order=photo.sort_order,
    )


@router.patch("/{part_id}/photos/reorder", response_model=list[PartPhotoResponse])
async def reorder_part_photos(
    part_id: int,
    payload: PhotoReorderRequest,
    service: Annotated[PartsService, Depends(get_parts_service)],
) -> list[PartPhotoResponse]:
    try:
        photos = await service.reorder_photos(part_id, payload.photo_ids)
    except PartNotFoundError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Part not found") from exc
    except ValueError as exc:
        raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_ENTITY, detail=str(exc)) from exc

    return [
        PartPhotoResponse(
            id=photo.id,
            part_id=photo.part_id,
            file_path=photo.file_path,
            url=service.storage.public_url(photo.file_path),
            sort_order=photo.sort_order,
        )
        for photo in photos
    ]


@router.delete("/{part_id}/photos/{photo_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_part_photo(
    part_id: int,
    photo_id: int,
    service: Annotated[PartsService, Depends(get_parts_service)],
) -> None:
    try:
        await service.delete_photo(part_id, photo_id)
    except PartNotFoundError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Part not found") from exc
    except PartPhotoNotFoundError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Photo not found") from exc
