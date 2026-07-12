from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles

import catalog.models  # noqa: F401 — register ORM models
import parts.models  # noqa: F401 — register ORM models
import tech_process.models  # noqa: F401 — register ORM models
from catalog.router import router as catalog_router
from assembly.router import router as assembly_router
from core.config import settings
from core.database import Base, engine
from core.telegram_auth import TelegramAuthMiddleware
from parts.router import router as parts_router
from tech_process.router import router as tech_process_router


@asynccontextmanager
async def lifespan(app: FastAPI):
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
    yield
    await engine.dispose()


app = FastAPI(title="Техпроцессы ЧПУ", lifespan=lifespan)

settings.uploads_dir.mkdir(parents=True, exist_ok=True)
app.mount(settings.uploads_url_prefix, StaticFiles(directory=settings.uploads_dir), name="uploads")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)
app.add_middleware(TelegramAuthMiddleware)

app.include_router(catalog_router, prefix=settings.api_v1_prefix)
app.include_router(parts_router, prefix=settings.api_v1_prefix)
app.include_router(tech_process_router, prefix=settings.api_v1_prefix)
app.include_router(assembly_router, prefix=settings.api_v1_prefix)


@app.get("/health")
async def health() -> dict[str, str]:
    return {"status": "OK"}
