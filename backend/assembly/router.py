from typing import Annotated, Literal

from fastapi import APIRouter, Depends, HTTPException, status
from pydantic import BaseModel
from sqlalchemy.ext.asyncio import AsyncSession

from assembly.service import AssemblyService, PartNotFoundError
from catalog.repository import CatalogRepository
from core.database import get_db
from parts.repository import PartsRepository
from tech_process.repository import TechProcessRepository

router = APIRouter(tags=["assembly"])


class RequiredItemResponse(BaseModel):
    id: int
    type: Literal["tool", "plate", "jaw"]
    name: str


class RequiredItemsResponse(BaseModel):
    tools: list[RequiredItemResponse]
    plates: list[RequiredItemResponse]
    jaws: list[RequiredItemResponse]


def get_assembly_service(
    session: Annotated[AsyncSession, Depends(get_db)],
) -> AssemblyService:
    return AssemblyService(
        PartsRepository(session),
        TechProcessRepository(session),
        CatalogRepository(session),
    )


@router.get("/parts/{part_id}/required-items", response_model=RequiredItemsResponse)
async def get_required_items(
    part_id: int,
    service: Annotated[AssemblyService, Depends(get_assembly_service)],
) -> RequiredItemsResponse:
    try:
        result = await service.get_required_items(part_id)
    except PartNotFoundError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Part not found") from exc

    return RequiredItemsResponse(
        tools=[
            RequiredItemResponse(id=item.id, type="tool", name=item.name) for item in result.tools
        ],
        plates=[
            RequiredItemResponse(id=item.id, type="plate", name=item.name) for item in result.plates
        ],
        jaws=[
            RequiredItemResponse(id=item.id, type="jaw", name=item.name) for item in result.jaws
        ],
    )
