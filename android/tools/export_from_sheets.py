#!/usr/bin/env python3
"""Export CNC tech-process data from Google Apps Script Web App into migration.zip.

Usage:
  python export_from_sheets.py [--api-url URL] [--out migration.zip]
"""

from __future__ import annotations

import argparse
import json
import sys
import zipfile
from datetime import datetime, timezone
from pathlib import Path
from typing import Any
from urllib.parse import urlparse

import requests

DEFAULT_API_URL = (
    "https://script.google.com/macros/s/"
    "AKfycbxD1AjO9kD26CNbEm_SyJoMjm1UkNYdh3kKleOFbc4WGnkQbLbB8oS_LLQ5AMOg1CzeUA/exec"
)


def api_request(
    session: requests.Session,
    api_url: str,
    path: str,
    method: str = "GET",
    query: dict[str, str] | None = None,
) -> tuple[bool, int, Any]:
    envelope = {
        "path": path,
        "method": method,
        "query": query or {},
        "body": None,
        "initData": None,
    }
    response = session.post(
        api_url,
        data=json.dumps(envelope),
        headers={"Content-Type": "text/plain;charset=utf-8"},
        timeout=120,
        allow_redirects=True,
    )
    try:
        payload = response.json()
    except Exception:
        return False, response.status_code, f"Non-JSON response HTTP {response.status_code}"
    ok = bool(payload.get("ok"))
    http_status = int(payload.get("httpStatus") or response.status_code)
    if ok:
        return True, http_status, payload.get("data")
    return False, http_status, payload.get("detail") or "Unknown API error"


def parse_created_at(value: Any) -> int:
    now_ms = int(datetime.now(tz=timezone.utc).timestamp() * 1000)
    if value is None:
        return now_ms
    if isinstance(value, (int, float)):
        # Already epoch? treat seconds vs ms heuristically
        n = int(value)
        return n if n > 10_000_000_000 else n * 1000
    text = str(value).strip()
    if not text:
        return now_ms
    if text.isdigit():
        n = int(text)
        return n if n > 10_000_000_000 else n * 1000
    try:
        # Support trailing Z
        normalized = text.replace("Z", "+00:00")
        dt = datetime.fromisoformat(normalized)
        if dt.tzinfo is None:
            dt = dt.replace(tzinfo=timezone.utc)
        return int(dt.timestamp() * 1000)
    except Exception:
        return now_ms


def ext_from_content_type(content_type: str | None) -> str:
    if not content_type:
        return "jpg"
    ct = content_type.split(";")[0].strip().lower()
    return {
        "image/jpeg": "jpg",
        "image/jpg": "jpg",
        "image/png": "png",
        "image/webp": "webp",
        "image/gif": "gif",
    }.get(ct, "jpg")


def download_photo(
    session: requests.Session,
    url: str,
    dest: Path,
) -> bool:
    try:
        resp = session.get(url, timeout=60, allow_redirects=True)
        if resp.status_code >= 400:
            print(f"WARN: photo HTTP {resp.status_code}: {url}", file=sys.stderr)
            return False
        dest.parent.mkdir(parents=True, exist_ok=True)
        dest.write_bytes(resp.content)
        return True
    except Exception as exc:
        print(f"WARN: photo download failed ({exc}): {url}", file=sys.stderr)
        return False


def main() -> int:
    parser = argparse.ArgumentParser(description="Export Sheets/Drive data to migration.zip")
    parser.add_argument("--api-url", default=DEFAULT_API_URL, help="Apps Script Web App /exec URL")
    parser.add_argument("--out", default="migration.zip", help="Output zip path")
    args = parser.parse_args()

    out_path = Path(args.out).resolve()
    work = out_path.parent / f".migration_export_{out_path.stem}"
    if work.exists():
        import shutil

        shutil.rmtree(work)
    photos_root = work / "photos"
    photos_root.mkdir(parents=True)

    session = requests.Session()
    failed_photos: list[str] = []

    print(f"API: {args.api_url}")
    ok, status, catalog = api_request(session, args.api_url, "/api/v1/catalog")
    if not ok:
        print(f"ERROR: catalog failed ({status}): {catalog}", file=sys.stderr)
        return 1
    if not isinstance(catalog, list):
        print(f"ERROR: unexpected catalog payload: {type(catalog)}", file=sys.stderr)
        return 1
    catalog = sorted(catalog, key=lambda x: int(x.get("id") or 0))
    print(f"Catalog items: {len(catalog)}")

    ok, status, parts = api_request(session, args.api_url, "/api/v1/parts")
    if not ok:
        print(f"ERROR: parts failed ({status}): {parts}", file=sys.stderr)
        return 1
    if not isinstance(parts, list):
        print(f"ERROR: unexpected parts payload: {type(parts)}", file=sys.stderr)
        return 1
    parts = sorted(parts, key=lambda x: int(x.get("id") or 0))
    print(f"Parts: {len(parts)}")

    export_catalog: list[dict[str, Any]] = []
    for item in catalog:
        item_id = int(item["id"])
        photo_rels: list[str] = []
        photos = item.get("photos")
        if isinstance(photos, list):
            for idx, photo in enumerate(photos):
                url = (photo or {}).get("url")
                if not url:
                    continue
                # Probe content-type via GET
                try:
                    head = session.get(url, timeout=60, stream=True, allow_redirects=True)
                    ext = ext_from_content_type(head.headers.get("Content-Type"))
                    head.close()
                except Exception:
                    ext = "jpg"
                rel = f"catalog/{item_id}/{idx}.{ext}"
                dest = photos_root / rel
                if download_photo(session, url, dest):
                    photo_rels.append(rel)
                else:
                    failed_photos.append(f"catalog#{item_id} photo#{idx}")
        export_catalog.append(
            {
                "id": item_id,
                "type": str(item.get("type")),
                "name": str(item.get("name") or ""),
                "note": item.get("note"),
                "photos": photo_rels,
            }
        )

    export_parts: list[dict[str, Any]] = []
    export_tps: list[dict[str, Any]] = []
    setup_total = 0
    op_total = 0
    photo_total = 0

    for part in parts:
        part_id = int(part["id"])
        photo_rels: list[str] = []
        photos = part.get("photos") or []
        if isinstance(photos, list):
            # keep API sort_order if present
            photos = sorted(photos, key=lambda p: int((p or {}).get("sort_order") or 0))
            for idx, photo in enumerate(photos):
                url = (photo or {}).get("url")
                if not url:
                    continue
                try:
                    head = session.get(url, timeout=60, stream=True, allow_redirects=True)
                    ext = ext_from_content_type(head.headers.get("Content-Type"))
                    head.close()
                except Exception:
                    ext = "jpg"
                rel = f"parts/{part_id}/{idx}.{ext}"
                dest = photos_root / rel
                if download_photo(session, url, dest):
                    photo_rels.append(rel)
                    photo_total += 1
                else:
                    failed_photos.append(f"part#{part_id} photo#{idx}")

        export_parts.append(
            {
                "id": part_id,
                "number": str(part.get("number") or ""),
                "title": str(part.get("title") or ""),
                "createdAt": parse_created_at(part.get("created_at")),
                "photos": photo_rels,
            }
        )

        ok, status, tp = api_request(
            session,
            args.api_url,
            f"/api/v1/parts/{part_id}/tech-process",
        )
        if not ok:
            if status == 404:
                print(f"Part {part_id}: no tech process (404) — skip")
                continue
            print(f"WARN: tech-process part#{part_id} failed ({status}): {tp}", file=sys.stderr)
            continue

        setups_out = []
        for setup in tp.get("setups") or []:
            ops_out = []
            for op in setup.get("operations") or []:
                ops_out.append(
                    {
                        "id": int(op["id"]),
                        "order": int(op.get("order") or 0),
                        "opNumber": str(op.get("op_number") or ""),
                        "title": str(op.get("title") or ""),
                        "toolId": int(op["tool_id"]),
                        "plateId": int(op["plate_id"]),
                        "comment": op.get("comment"),
                    }
                )
                op_total += 1
            setups_out.append(
                {
                    "id": int(setup["id"]),
                    "order": int(setup.get("order") or 0),
                    "jawId": int(setup["jaw_id"]),
                    "operations": ops_out,
                }
            )
            setup_total += 1
        export_tps.append(
            {
                "id": int(tp["id"]),
                "partId": int(tp.get("part_id") or part_id),
                "setups": setups_out,
            }
        )

    # catalog photo count for summary
    catalog_photo_total = sum(len(c["photos"]) for c in export_catalog)
    photo_total += catalog_photo_total

    data = {
        "parts": export_parts,
        "catalogItems": export_catalog,
        "techProcesses": export_tps,
    }
    data_path = work / "data.json"
    data_path.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")

    with zipfile.ZipFile(out_path, "w", compression=zipfile.ZIP_DEFLATED) as zf:
        zf.write(data_path, arcname="data.json")
        for file_path in photos_root.rglob("*"):
            if file_path.is_file():
                arc = file_path.relative_to(work).as_posix()
                zf.write(file_path, arcname=arc)

    import shutil

    shutil.rmtree(work, ignore_errors=True)

    print("---")
    print(f"Wrote: {out_path}")
    print(f"Parts: {len(export_parts)}")
    print(f"Catalog items: {len(export_catalog)}")
    print(f"Tech processes: {len(export_tps)}")
    print(f"Setups: {setup_total}")
    print(f"Operations: {op_total}")
    print(f"Photos exported: {photo_total}")
    print(f"Photos failed: {len(failed_photos)}")
    for item in failed_photos:
        print(f"  - {item}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
