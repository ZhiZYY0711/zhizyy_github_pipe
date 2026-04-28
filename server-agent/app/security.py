from typing import Any

from fastapi import Depends, HTTPException
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from jose import JWTError, jwt

from app.core.config import get_settings

bearer_scheme = HTTPBearer(auto_error=False)


def verify_internal_token(token: str, expected_audience: str | None = None) -> dict[str, Any]:
    settings = get_settings()
    try:
        return jwt.decode(
            token,
            settings.internal_jwt_secret,
            algorithms=["HS256"],
            audience=expected_audience or settings.internal_jwt_audience,
        )
    except JWTError as exc:
        raise ValueError("invalid internal token") from exc


async def require_internal_token(
    credentials: HTTPAuthorizationCredentials | None = Depends(bearer_scheme),
) -> dict[str, Any] | None:
    settings = get_settings()
    if not settings.require_internal_auth:
        return None
    if credentials is None:
        raise HTTPException(status_code=401, detail="Missing internal token")
    try:
        return verify_internal_token(credentials.credentials, settings.internal_jwt_audience)
    except ValueError as exc:
        raise HTTPException(status_code=401, detail="Invalid internal token") from exc
