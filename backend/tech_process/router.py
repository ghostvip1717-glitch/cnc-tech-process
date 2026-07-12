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
    InvalidPlateError,
    InvalidToolError,
    OperationNotFoundError,
    PartNotFoundError,
    SetupNotFoundError,
    TechProcessAlreadyExistsError,
    TechProcessNotFoundError,
    TechProcessService,
)
from tech_process.utils import setup_order_label

router = APIRouter(tags=["tech_process"])


class OperationResponse(BaseModel):
    id: int
    setup_id: int
    order: int
    op_number: str
    title: str
    tool_id: int
    plate_id: int
    comment: str | None


class SetupResponse(BaseModel):
    id: int
    tech_process_id: int
    order: int
    order_label: str
    jaw_id: int
    operations: list[OperationResponse]


class TechProcessResponse(BaseModel):
    id: int
    part_id: int
    setups: list[SetupResponse]


class SetupCreateRequest(BaseModel):
    jaw_id: int = Field(gt=0)


class SetupUpdateRequest(BaseModel):
    jaw_id: int = Field(gt=0)


class OperationCreateRequest(BaseModel):
    op_number: str = Field(min_length=1, max_length=16)
    title: str = Field(min_length=1, max_length=500)
    tool_id: int = Field(gt=0)
    plate_id: int = Field(gt=0)
    comment: str | None = Field(default=None, max_length=1000)


class OperationUpdateRequest(BaseModel):
    op_number: str | None = Field(default=None, min_length=1, max_length=16)
    title: str | None = Field(default=None, min_length=1, max_length=500)
    tool_id: int | None = Field(default=None, gt=0)
    plate_id: int | None = Field(default=None, gt=0)
    comment: str | None = Field(default=None, max_length=1000)


class OperationReorderRequest(BaseModel):
    operation_ids: list[int] = Field(min_length=1)


def get_tech_process_service(
    session: Annotated[AsyncSession, Depends(get_db)],
) -> TechProcessService:
    return TechProcessService(
        TechProcessRepository(session),
        PartsRepository(session),
        CatalogRepository(session),
    )


def _operation_to_response(operation) -> OperationResponse:
    return OperationResponse(
        id=operation.id,
        setup_id=operation.setup_id,
        order=operation.order,
        op_number=operation.op_number,
        title=operation.title,
        tool_id=operation.tool_id,
        plate_id=operation.plate_id,
        comment=operation.comment,
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
                operations=[_operation_to_response(operation) for operation in setup.operations],
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
        operations=[],
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
        operations=[_operation_to_response(operation) for operation in setup.operations],
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


@router.post(
    "/parts/{part_id}/tech-process/setups/{setup_id}/operations",
    response_model=OperationResponse,
    status_code=status.HTTP_201_CREATED,
)
async def create_operation(
    part_id: int,
    setup_id: int,
    payload: OperationCreateRequest,
    service: Annotated[TechProcessService, Depends(get_tech_process_service)],
) -> OperationResponse:
    try:
        operation = await service.add_operation(
            part_id,
            setup_id,
            payload.op_number,
            payload.title,
            payload.tool_id,
            payload.plate_id,
            payload.comment,
        )
    except PartNotFoundError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Part not found") from exc
    except TechProcessNotFoundError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Tech process not found") from exc
    except SetupNotFoundError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Setup not found") from exc
    except (InvalidToolError, InvalidPlateError) as exc:
        detail = "Invalid tool_id" if isinstance(exc, InvalidToolError) else "Invalid plate_id"
        raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_ENTITY, detail=detail) from exc
    except ValueError as exc:
        raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_ENTITY, detail=str(exc)) from exc

    return _operation_to_response(operation)


@router.patch(
    "/parts/{part_id}/tech-process/operations/{operation_id}",
    response_model=OperationResponse,
)
async def update_operation(
    part_id: int,
    operation_id: int,
    payload: OperationUpdateRequest,
    service: Annotated[TechProcessService, Depends(get_tech_process_service)],
) -> OperationResponse:
    if all(
        value is None
        for value in (payload.op_number, payload.title, payload.tool_id, payload.plate_id, payload.comment)
    ):
        raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_ENTITY, detail="No fields to update")

    try:
        operation = await service.update_operation(
            part_id,
            operation_id,
            payload.op_number,
            payload.title,
            payload.tool_id,
            payload.plate_id,
            payload.comment,
        )
    except PartNotFoundError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Part not found") from exc
    except OperationNotFoundError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Operation not found") from exc
    except (InvalidToolError, InvalidPlateError) as exc:
        detail = "Invalid tool_id" if isinstance(exc, InvalidToolError) else "Invalid plate_id"
        raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_ENTITY, detail=detail) from exc
    except ValueError as exc:
        raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_ENTITY, detail=str(exc)) from exc

    return _operation_to_response(operation)


@router.delete(
    "/parts/{part_id}/tech-process/operations/{operation_id}",
    status_code=status.HTTP_204_NO_CONTENT,
)
async def delete_operation(
    part_id: int,
    operation_id: int,
    service: Annotated[TechProcessService, Depends(get_tech_process_service)],
) -> None:
    try:
        await service.delete_operation(part_id, operation_id)
    except PartNotFoundError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Part not found") from exc
    except OperationNotFoundError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Operation not found") from exc


@router.patch(
    "/parts/{part_id}/tech-process/setups/{setup_id}/operations/reorder",
    response_model=list[OperationResponse],
)
async def reorder_operations(
    part_id: int,
    setup_id: int,
    payload: OperationReorderRequest,
    service: Annotated[TechProcessService, Depends(get_tech_process_service)],
) -> list[OperationResponse]:
    try:
        operations = await service.reorder_operations(part_id, setup_id, payload.operation_ids)
    except PartNotFoundError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Part not found") from exc
    except TechProcessNotFoundError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Tech process not found") from exc
    except SetupNotFoundError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Setup not found") from exc
    except ValueError as exc:
        raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_ENTITY, detail=str(exc)) from exc

    return [_operation_to_response(operation) for operation in operations]
