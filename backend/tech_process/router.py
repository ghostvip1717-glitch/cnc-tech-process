from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException, status
from pydantic import BaseModel, Field
from sqlalchemy.ext.asyncio import AsyncSession

from catalog.repository import CatalogRepository
from core.database import get_db
from parts.repository import PartsRepository
from tech_process.repository import TechProcessRepository
from tech_process.service import (
    InvalidJawError,
    PartNotFoundError,
    SetupNotFoundError,
    TechProcessAlreadyExistsError,
    TechProcessNotFoundError,
    TechProcessService,
)
from tech_process.utils import setup_order_label

router = APIRouter(tags=["tech_process"])


class SetupResponse(BaseModel):
    id: int
    tech_process_id: int
    order: int
    order_label: str
    jaw_id: int


class TechProcessResponse(BaseModel):
    id: int
    part_id: int
    setups: list[SetupResponse]


class SetupCreateRequest(BaseModel):
    jaw_id: int = Field(gt=0)


class SetupUpdateRequest(BaseModel):
    jaw_id: int = Field(gt=0)


def get_tech_process_service(
    session: Annotated[AsyncSession, Depends(get_db)],
) -> TechProcessService:
    return TechProcessService(
        TechProcessRepository(session),
        PartsRepository(session),
        CatalogRepository(session),
    )


def _tech_process_to_response(tech_process) -> TechProcessResponse:
    return TechProcessResponse(
        id=tech_process.id,
        part_id=tech_process.part_id,
        setups=[
            SetupResponse(
                id=setup.id,
                tech_process_id=setup.tech_process_id,
                order=setup.order,
                order_label=setup_order_label(setup.order),
                jaw_id=setup.jaw_id,
            )
            for setup in tech_process.setups
        ],
    )


@router.get("/parts/{part_id}/tech-process", response_model=TechProcessResponse)
async def get_part_tech_process(
    part_id: int,
    service: Annotated[TechProcessService, Depends(get_tech_process_service)],
) -> TechProcessResponse:
    try:
        tech_process = await service.get_tech_process(part_id)
    except PartNotFoundError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Part not found") from exc
    except TechProcessNotFoundError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Tech process not found") from exc

    return _tech_process_to_response(tech_process)


@router.put("/parts/{part_id}/tech-process", response_model=TechProcessResponse)
async def create_part_tech_process(
    part_id: int,
    service: Annotated[TechProcessService, Depends(get_tech_process_service)],
) -> TechProcessResponse:
    try:
        tech_process = await service.create_tech_process(part_id)
    except PartNotFoundError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Part not found") from exc
    except TechProcessAlreadyExistsError as exc:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Tech process already exists for this part",
        ) from exc

    return _tech_process_to_response(tech_process)


@router.post(
    "/parts/{part_id}/tech-process/setups",
    response_model=SetupResponse,
    status_code=status.HTTP_201_CREATED,
)
async def create_setup(
    part_id: int,
    payload: SetupCreateRequest,
    service: Annotated[TechProcessService, Depends(get_tech_process_service)],
) -> SetupResponse:
    try:
        setup = await service.add_setup(part_id, payload.jaw_id)
    except PartNotFoundError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Part not found") from exc
    except InvalidJawError as exc:
        raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_ENTITY, detail="Invalid jaw_id") from exc

    return SetupResponse(
        id=setup.id,
        tech_process_id=setup.tech_process_id,
        order=setup.order,
        order_label=setup_order_label(setup.order),
        jaw_id=setup.jaw_id,
    )


@router.patch("/parts/{part_id}/tech-process/setups/{setup_id}", response_model=SetupResponse)
async def update_setup(
    part_id: int,
    setup_id: int,
    payload: SetupUpdateRequest,
    service: Annotated[TechProcessService, Depends(get_tech_process_service)],
) -> SetupResponse:
    try:
        setup = await service.update_setup(part_id, setup_id, payload.jaw_id)
    except PartNotFoundError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Part not found") from exc
    except TechProcessNotFoundError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Tech process not found") from exc
    except SetupNotFoundError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Setup not found") from exc
    except InvalidJawError as exc:
        raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_ENTITY, detail="Invalid jaw_id") from exc

    return SetupResponse(
        id=setup.id,
        tech_process_id=setup.tech_process_id,
        order=setup.order,
        order_label=setup_order_label(setup.order),
        jaw_id=setup.jaw_id,
    )


@router.delete("/parts/{part_id}/tech-process/setups/{setup_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_setup(
    part_id: int,
    setup_id: int,
    service: Annotated[TechProcessService, Depends(get_tech_process_service)],
) -> None:
    try:
        await service.delete_setup(part_id, setup_id)
    except PartNotFoundError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Part not found") from exc
    except TechProcessNotFoundError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Tech process not found") from exc
    except SetupNotFoundError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Setup not found") from exc
