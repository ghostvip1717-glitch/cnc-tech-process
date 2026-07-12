from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException, Query, status
from pydantic import BaseModel, Field
from sqlalchemy.ext.asyncio import AsyncSession

from catalog.models import CatalogItemType
from catalog.repository import CatalogRepository
from catalog.service import CatalogItemNotFoundError, CatalogNameConflictError, CatalogService
from core.database import get_db

router = APIRouter(prefix="/catalog", tags=["catalog"])


class CatalogItemResponse(BaseModel):
    id: int
    type: CatalogItemType
    name: str
    note: str | None

    model_config = {"from_attributes": True}


class CatalogItemCreateRequest(BaseModel):
    type: CatalogItemType
    name: str = Field(min_length=1, max_length=255)
    note: str | None = Field(default=None, max_length=1000)


class CatalogItemUpdateRequest(BaseModel):
    name: str | None = Field(default=None, min_length=1, max_length=255)
    note: str | None = Field(default=None, max_length=1000)


def get_catalog_service(
    session: Annotated[AsyncSession, Depends(get_db)],
) -> CatalogService:
    return CatalogService(CatalogRepository(session))


@router.get("", response_model=list[CatalogItemResponse])
async def list_catalog_items(
    service: Annotated[CatalogService, Depends(get_catalog_service)],
    item_type: Annotated[CatalogItemType | None, Query(alias="type")] = None,
    q: Annotated[str | None, Query()] = None,
) -> list[CatalogItemResponse]:
    items = await service.list_items(item_type, q)
    return [CatalogItemResponse.model_validate(item) for item in items]


@router.post("", response_model=CatalogItemResponse, status_code=status.HTTP_201_CREATED)
async def create_catalog_item(
    payload: CatalogItemCreateRequest,
    service: Annotated[CatalogService, Depends(get_catalog_service)],
) -> CatalogItemResponse:
    try:
        item = await service.create_item(payload.type, payload.name, payload.note)
    except CatalogNameConflictError as exc:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Item with this name already exists for the selected type",
        ) from exc
    except ValueError as exc:
        raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_ENTITY, detail=str(exc)) from exc

    return CatalogItemResponse.model_validate(item)


@router.get("/{item_id}", response_model=CatalogItemResponse)
async def get_catalog_item(
    item_id: int,
    service: Annotated[CatalogService, Depends(get_catalog_service)],
) -> CatalogItemResponse:
    try:
        item = await service.get_item(item_id)
    except CatalogItemNotFoundError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Catalog item not found") from exc

    return CatalogItemResponse.model_validate(item)


@router.patch("/{item_id}", response_model=CatalogItemResponse)
async def update_catalog_item(
    item_id: int,
    payload: CatalogItemUpdateRequest,
    service: Annotated[CatalogService, Depends(get_catalog_service)],
) -> CatalogItemResponse:
    if payload.name is None and payload.note is None:
        raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_ENTITY, detail="No fields to update")

    try:
        item = await service.update_item(item_id, payload.name, payload.note)
    except CatalogItemNotFoundError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Catalog item not found") from exc
    except CatalogNameConflictError as exc:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Item with this name already exists for the selected type",
        ) from exc
    except ValueError as exc:
        raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_ENTITY, detail=str(exc)) from exc

    return CatalogItemResponse.model_validate(item)


@router.delete("/{item_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_catalog_item(
    item_id: int,
    service: Annotated[CatalogService, Depends(get_catalog_service)],
) -> None:
    try:
        await service.delete_item(item_id)
    except CatalogItemNotFoundError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Catalog item not found") from exc
