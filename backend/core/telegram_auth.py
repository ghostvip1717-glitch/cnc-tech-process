import hashlib
import hmac
import json
import time
from typing import Any
from urllib.parse import parse_qsl, unquote

from starlette.middleware.base import BaseHTTPMiddleware, RequestResponseEndpoint
from starlette.requests import Request
from starlette.responses import JSONResponse, Response

from core.config import settings

INIT_DATA_HEADER = "X-Telegram-Init-Data"
MAX_AUTH_AGE_SECONDS = 86_400


def _parse_init_data(init_data: str) -> tuple[dict[str, str], str]:
    pairs = parse_qsl(init_data, keep_blank_values=True)
    data: dict[str, str] = {key: value for key, value in pairs}
    received_hash = data.pop("hash", None)
    if received_hash is None:
        raise ValueError("hash is missing")
    return data, received_hash


def validate_telegram_init_data(init_data: str, bot_token: str) -> dict[str, Any]:
    if not init_data:
        raise ValueError("init data is empty")
    if not bot_token:
        raise ValueError("bot token is not configured")

    data, received_hash = _parse_init_data(init_data)
    data_check_string = "\n".join(f"{key}={value}" for key, value in sorted(data.items()))
    secret_key = hmac.new(b"WebAppData", bot_token.encode(), hashlib.sha256).digest()
    calculated_hash = hmac.new(secret_key, data_check_string.encode(), hashlib.sha256).hexdigest()

    if not hmac.compare_digest(calculated_hash, received_hash):
        raise ValueError("invalid init data signature")

    auth_date_raw = data.get("auth_date")
    if auth_date_raw is not None:
        auth_date = int(auth_date_raw)
        if time.time() - auth_date > MAX_AUTH_AGE_SECONDS:
            raise ValueError("init data is expired")

    user_raw = data.get("user")
    if not user_raw:
        raise ValueError("user is missing")

    user = json.loads(unquote(user_raw))
    if "id" not in user:
        raise ValueError("user id is missing")

    return user


def _is_exempt_path(path: str) -> bool:
    if path == "/health":
        return True
    if path in {"/docs", "/openapi.json", "/redoc"}:
        return True
    if path.startswith(f"{settings.uploads_url_prefix}/"):
        return True
    return False


class TelegramAuthMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next: RequestResponseEndpoint) -> Response:
        if not settings.telegram_auth_enabled or _is_exempt_path(request.url.path):
            return await call_next(request)

        if not request.url.path.startswith(settings.api_v1_prefix):
            return await call_next(request)

        init_data = request.headers.get(INIT_DATA_HEADER)
        if not init_data:
            return JSONResponse(status_code=401, content={"detail": "Missing Telegram init data"})

        try:
            user = validate_telegram_init_data(init_data, settings.bot_token)
        except ValueError as exc:
            return JSONResponse(status_code=401, content={"detail": str(exc)})

        if settings.telegram_allowed_user_ids:
            user_id = int(user["id"])
            if user_id not in settings.telegram_allowed_user_ids:
                return JSONResponse(status_code=403, content={"detail": "User not allowed"})

        request.state.telegram_user = user
        return await call_next(request)
