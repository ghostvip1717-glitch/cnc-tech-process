import uuid
from pathlib import Path

from fastapi import UploadFile

from core.config import settings

ALLOWED_CONTENT_TYPES = {
    "image/jpeg": ".jpg",
    "image/png": ".png",
    "image/webp": ".webp",
    "image/gif": ".gif",
}


class PhotoStorage:
    def __init__(self, base_dir: Path | None = None) -> None:
        self.base_dir = base_dir or settings.uploads_dir
        self.base_dir.mkdir(parents=True, exist_ok=True)

    def _part_dir(self, part_id: int) -> Path:
        part_dir = self.base_dir / "parts" / str(part_id)
        part_dir.mkdir(parents=True, exist_ok=True)
        return part_dir

    async def save_photo(self, part_id: int, upload: UploadFile) -> str:
        if upload.content_type not in ALLOWED_CONTENT_TYPES:
            raise ValueError("Unsupported image type")

        extension = ALLOWED_CONTENT_TYPES[upload.content_type]
        filename = f"{uuid.uuid4().hex}{extension}"
        destination = self._part_dir(part_id) / filename

        content = await upload.read()
        if not content:
            raise ValueError("Empty file")

        destination.write_bytes(content)
        return str(destination.relative_to(self.base_dir)).replace("\\", "/")

    def delete_photo(self, file_path: str) -> None:
        full_path = self.base_dir / file_path
        if full_path.is_file():
            full_path.unlink()

    def delete_part_photos(self, part_id: int) -> None:
        part_dir = self.base_dir / "parts" / str(part_id)
        if part_dir.is_dir():
            for file in part_dir.iterdir():
                if file.is_file():
                    file.unlink()

    def public_url(self, file_path: str) -> str:
        return f"{settings.uploads_url_prefix}/{file_path}"
