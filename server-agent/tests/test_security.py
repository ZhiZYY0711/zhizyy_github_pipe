import pytest
from jose import jwt

from app.core.config import get_settings
from app.security import verify_internal_token


def test_verify_internal_token_accepts_expected_audience() -> None:
    settings = get_settings()
    token = jwt.encode(
        {"sub": "server-web", "aud": settings.internal_jwt_audience},
        settings.internal_jwt_secret,
        algorithm="HS256",
    )

    claims = verify_internal_token(token, expected_audience=settings.internal_jwt_audience)

    assert claims["sub"] == "server-web"


def test_verify_internal_token_rejects_wrong_audience() -> None:
    settings = get_settings()
    token = jwt.encode(
        {"sub": "server-web", "aud": "wrong-audience"},
        settings.internal_jwt_secret,
        algorithm="HS256",
    )

    with pytest.raises(ValueError):
        verify_internal_token(token, expected_audience=settings.internal_jwt_audience)
