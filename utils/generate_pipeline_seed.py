#!/usr/bin/env python3
"""Generate topology-aware MySQL seed SQL for pipeline demo data."""

from __future__ import annotations

import argparse
import json
import random
import re
from dataclasses import dataclass
from datetime import datetime, timedelta
from pathlib import Path
from typing import Any, Iterable


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_BLUEPRINT = ROOT / "docs" / "database" / "seed" / "pipeline-topology-blueprint.json"
DEFAULT_OUTPUT = ROOT / "docs" / "database" / "generated" / "pipeline-topology-seed.sql"
DEFAULT_AREA_SQL = ROOT / "docs" / "database" / "01-init" / "area.sql"
AREA_INSERT_RE = re.compile(r"INSERT INTO `area` VALUES \((\d+), '([^']*)', '([^']*)', '([^']*)',")


@dataclass(frozen=True)
class AreaNode:
    area_id: int
    name: str


@dataclass(frozen=True)
class PipeDef:
    id: int
    name: str
    pipe_level: str
    segment_level: str
    medium: str
    description: str
    route: list[AreaNode]

    @property
    def start_area_id(self) -> int:
        return self.route[0].area_id

    @property
    def end_area_id(self) -> int:
        return self.route[-1].area_id


@dataclass(frozen=True)
class RouteNodeRow:
    id: int
    pipe_id: int
    area_id: int
    area_name: str
    node_order: int
    node_role: str


@dataclass(frozen=True)
class SegmentRow:
    id: int
    pipe_id: int
    segment_order: int
    start_node_id: int
    end_node_id: int
    start_area_id: int
    end_area_id: int
    start_area_name: str
    end_area_name: str
    segment_level: str
    name: str
    medium: str


@dataclass(frozen=True)
class AreaRow:
    area_id: int
    province: str
    city: str
    district: str

    @property
    def display_name(self) -> str:
        if self.district != "-1":
            return self.district
        if self.city != "-1":
            return self.city
        return self.province


def sql_str(value: Any) -> str:
    if value is None:
        return "NULL"
    text = str(value).replace("\\", "\\\\").replace("'", "''")
    return f"'{text}'"


def sql_dt(value: datetime) -> str:
    return sql_str(value.strftime("%Y-%m-%d %H:%M:%S"))


def load_blueprint(path: Path, profile: str) -> tuple[list[PipeDef], dict[str, int]]:
    with path.open("r", encoding="utf-8") as fh:
        raw = json.load(fh)

    profiles = raw.get("profiles", {})
    if profile not in profiles:
        known = ", ".join(sorted(profiles))
        raise ValueError(f"Unknown profile {profile!r}. Available profiles: {known}")

    pipe_defs: list[PipeDef] = []
    for index, item in enumerate(raw.get("pipes", []), start=1):
        required = ["name", "pipe_level", "segment_level", "medium", "description", "route"]
        missing = [field for field in required if field not in item]
        if missing:
            raise ValueError(f"Pipe #{index} is missing required fields: {', '.join(missing)}")

        route = [AreaNode(area_id=int(node["area_id"]), name=str(node["name"])) for node in item["route"]]
        if len(route) < 2:
            raise ValueError(f"Pipe {item['name']} must contain at least two route nodes")
        if item["segment_level"] == "IN_DISTRICT" and route[0].area_id != route[-1].area_id:
            raise ValueError(f"IN_DISTRICT pipe {item['name']} must start and end in the same district")

        pipe_defs.append(
            PipeDef(
                id=index,
                name=str(item["name"]),
                pipe_level=str(item["pipe_level"]),
                segment_level=str(item["segment_level"]),
                medium=str(item["medium"]),
                description=str(item["description"]),
                route=route,
            )
        )

    if not pipe_defs:
        raise ValueError("Blueprint must contain at least one pipe")

    return pipe_defs, {key: int(value) for key, value in profiles[profile].items()}


def load_areas(path: Path) -> list[AreaRow]:
    rows: list[AreaRow] = []
    with path.open("r", encoding="utf-8") as fh:
        for line in fh:
            match = AREA_INSERT_RE.search(line)
            if not match:
                continue
            rows.append(
                AreaRow(
                    area_id=int(match.group(1)),
                    province=match.group(2),
                    city=match.group(3),
                    district=match.group(4),
                )
            )
    if not rows:
        raise ValueError(f"No area rows found in {path}")
    return rows


def city_rows_by_province(areas: list[AreaRow]) -> dict[str, list[AreaRow]]:
    grouped: dict[str, list[AreaRow]] = {}
    province_rows = {row.province: row for row in areas if row.city == "-1" and row.district == "-1"}
    for row in areas:
        if row.city != "-1" and row.district == "-1":
            grouped.setdefault(row.province, []).append(row)
    for province, province_row in province_rows.items():
        if province.endswith("市") and province not in grouped:
            grouped[province] = [province_row]
    return {province: sorted(rows, key=lambda item: item.area_id) for province, rows in grouped.items()}


def district_rows_by_city(areas: list[AreaRow]) -> dict[tuple[str, str], list[AreaRow]]:
    grouped: dict[tuple[str, str], list[AreaRow]] = {}
    for row in areas:
        if row.district == "-1":
            continue
        city_key = row.city if row.city != "-1" else row.province
        grouped.setdefault((row.province, city_key), []).append(row)
    return {key: sorted(rows, key=lambda item: item.area_id) for key, rows in grouped.items() if len(rows) >= 1}


def next_medium(index: int) -> str:
    return ["WATER", "GAS", "WATER", "GAS", "OIL"][index % 5]


def medium_label(medium: str) -> str:
    return {"WATER": "供水", "GAS": "燃气", "OIL": "成品油"}.get(medium, "综合")


def pipe_suffix(medium: str) -> str:
    return {"WATER": "W", "GAS": "G", "OIL": "O"}.get(medium, "P")


def make_pipe(pipe_id: int, name: str, pipe_level: str, segment_level: str, medium: str, description: str, route: list[AreaNode]) -> PipeDef:
    return PipeDef(
        id=pipe_id,
        name=name[:80],
        pipe_level=pipe_level,
        segment_level=segment_level,
        medium=medium,
        description=description[:255],
        route=route,
    )


def expand_pipes(base_pipes: list[PipeDef], target_count: int, areas: list[AreaRow], seed: int) -> list[PipeDef]:
    if target_count <= len(base_pipes):
        return base_pipes[:target_count]

    rng = random.Random(seed)
    pipes = list(base_pipes)
    used_names = {pipe.name for pipe in pipes}
    cities_by_province = city_rows_by_province(areas)
    districts_by_city = district_rows_by_city(areas)

    city_groups = [rows for rows in cities_by_province.values() if len(rows) >= 2]
    district_groups = [rows for rows in districts_by_city.values() if len(rows) >= 2]
    local_districts = [row for rows in districts_by_city.values() for row in rows]

    if not city_groups or not district_groups or not local_districts:
        raise ValueError("Area data does not contain enough city or district rows for expansion")

    regional_target = max(len(base_pipes), int(target_count * 0.28))
    branch_target = max(regional_target, int(target_count * 0.72))
    next_id = len(pipes) + 1
    serial = 1

    while len(pipes) < regional_target:
        group = city_groups[serial % len(city_groups)]
        route_len = min(len(group), 2 + (serial % 3))
        start = (serial * 2) % (len(group) - route_len + 1)
        route_rows = group[start : start + route_len]
        medium = next_medium(serial)
        route = [AreaNode(row.area_id, row.display_name) for row in route_rows]
        name = f"{route[0].name}-{route[-1].name}{medium_label(medium)}区域线-{pipe_suffix(medium)}{serial:04d}"
        if name in used_names:
            serial += 1
            continue
        pipes.append(
            make_pipe(
                next_id,
                name,
                "REGIONAL",
                "CITY_TO_CITY",
                medium,
                f"{route[0].name}至{route[-1].name}的同省城市级{medium_label(medium)}区域管道",
                route,
            )
        )
        used_names.add(name)
        next_id += 1
        serial += 1

    while len(pipes) < branch_target:
        group = district_groups[(serial * 3) % len(district_groups)]
        route_len = min(len(group), 2 + (serial % 3))
        start = (serial * 5) % (len(group) - route_len + 1)
        route_rows = group[start : start + route_len]
        medium = next_medium(serial + 1)
        route = [AreaNode(row.area_id, row.display_name) for row in route_rows]
        city_name = route_rows[0].city if route_rows[0].city != "-1" else route_rows[0].province
        name = f"{city_name}{route[0].name}-{route[-1].name}{medium_label(medium)}支线-{pipe_suffix(medium)}{serial:04d}"
        if name in used_names:
            serial += 1
            continue
        pipes.append(
            make_pipe(
                next_id,
                name,
                "BRANCH",
                "DISTRICT_TO_DISTRICT",
                medium,
                f"{city_name}内{route[0].name}至{route[-1].name}的区县级{medium_label(medium)}支线",
                route,
            )
        )
        used_names.add(name)
        next_id += 1
        serial += 1

    while len(pipes) < target_count:
        row = local_districts[(serial * 7) % len(local_districts)]
        medium = next_medium(serial + 2)
        city_name = row.city if row.city != "-1" else row.province
        route = [AreaNode(row.area_id, row.display_name), AreaNode(row.area_id, row.display_name)]
        name = f"{city_name}{row.display_name}{medium_label(medium)}本地线-{pipe_suffix(medium)}{serial:04d}"
        if name in used_names:
            serial += 1
            continue
        pipes.append(
            make_pipe(
                next_id,
                name,
                "LOCAL",
                "IN_DISTRICT",
                medium,
                f"{city_name}{row.display_name}内部{medium_label(medium)}本地管道",
                route,
            )
        )
        used_names.add(name)
        next_id += 1
        serial += 1

    generated = pipes[len(base_pipes) :]
    rng.shuffle(generated)
    pipes = base_pipes + generated
    return [make_pipe(index, pipe.name, pipe.pipe_level, pipe.segment_level, pipe.medium, pipe.description, pipe.route) for index, pipe in enumerate(pipes, start=1)]


def build_topology(pipes: list[PipeDef]) -> tuple[list[RouteNodeRow], list[SegmentRow]]:
    route_nodes: list[RouteNodeRow] = []
    segments: list[SegmentRow] = []
    next_node_id = 1
    next_segment_id = 1

    for pipe in pipes:
        pipe_node_ids: list[int] = []
        for index, area in enumerate(pipe.route, start=1):
            if index == 1:
                role = "START"
            elif index == len(pipe.route):
                role = "END"
            else:
                role = "VIA"
            route_nodes.append(
                RouteNodeRow(
                    id=next_node_id,
                    pipe_id=pipe.id,
                    area_id=area.area_id,
                    area_name=area.name,
                    node_order=index,
                    node_role=role,
                )
            )
            pipe_node_ids.append(next_node_id)
            next_node_id += 1

        for index in range(len(pipe.route) - 1):
            start_area = pipe.route[index]
            end_area = pipe.route[index + 1]
            if pipe.segment_level == "IN_DISTRICT":
                segment_name = f"{pipe.name}-内部段-S{index + 1}"
            else:
                segment_name = f"{start_area.name}-{end_area.name}段"
            segments.append(
                SegmentRow(
                    id=next_segment_id,
                    pipe_id=pipe.id,
                    segment_order=index + 1,
                    start_node_id=pipe_node_ids[index],
                    end_node_id=pipe_node_ids[index + 1],
                    start_area_id=start_area.area_id,
                    end_area_id=end_area.area_id,
                    start_area_name=start_area.name,
                    end_area_name=end_area.name,
                    segment_level=pipe.segment_level,
                    name=segment_name,
                    medium=pipe.medium,
                )
            )
            next_segment_id += 1

    return route_nodes, segments


def choose_area(segment: SegmentRow, index: int) -> int:
    if segment.segment_level == "IN_DISTRICT":
        return segment.start_area_id
    return segment.start_area_id if index % 2 == 0 else segment.end_area_id


def medium_sensor_type(medium: str, index: int) -> int:
    base = {"WATER": 0, "GAS": 1, "OIL": 2}.get(medium, 3)
    return (base + index) % 4


def status_for(index: int) -> int:
    if index % 97 == 0:
        return 2
    if index % 19 == 0:
        return 1
    return 0


def data_status_for(index: int) -> int:
    if index % 211 == 0:
        return 3
    if index % 47 == 0:
        return 2
    if index % 11 == 0:
        return 1
    return 0


def metric_values(segment: SegmentRow, index: int, rng: random.Random) -> tuple[float, float, float, float, int]:
    status = data_status_for(index)
    if segment.medium == "GAS":
        pressure = rng.uniform(2.0, 5.5) + status * 1.4
        traffic = rng.uniform(80.0, 180.0) + status * 24.0
        temperature = rng.uniform(14.0, 32.0) + status * 4.0
    elif segment.medium == "OIL":
        pressure = rng.uniform(4.0, 8.5) + status * 1.8
        traffic = rng.uniform(120.0, 260.0) + status * 36.0
        temperature = rng.uniform(18.0, 42.0) + status * 5.0
    else:
        pressure = rng.uniform(0.5, 1.8) + status * 0.7
        traffic = rng.uniform(60.0, 160.0) + status * 20.0
        temperature = rng.uniform(8.0, 28.0) + status * 3.5
    shake = rng.uniform(0.02, 1.2) + status * 0.9
    return round(pressure, 2), round(temperature, 2), round(traffic, 2), round(shake, 2), status


def batched(values: list[tuple[Any, ...]], size: int = 500) -> Iterable[list[tuple[Any, ...]]]:
    for index in range(0, len(values), size):
        yield values[index : index + size]


def write_insert(lines: list[str], table: str, columns: list[str], rows: list[tuple[Any, ...]]) -> None:
    if not rows:
        return
    column_sql = ", ".join(f"`{column}`" for column in columns)
    for batch in batched(rows):
        lines.append(f"INSERT INTO `{table}` ({column_sql}) VALUES")
        rendered = []
        for row in batch:
            rendered.append("  (" + ", ".join(render_value(value) for value in row) + ")")
        lines.append(",\n".join(rendered) + ";")
        lines.append("")


def render_value(value: Any) -> str:
    if isinstance(value, datetime):
        return sql_dt(value)
    if value is None:
        return "NULL"
    if isinstance(value, str):
        return sql_str(value)
    return str(value)


def schema_sql() -> list[str]:
    return [
        "-- Topology schema migration. Run once before reseeding topology data.",
        "ALTER TABLE `pipe` ADD COLUMN `pipe_level` VARCHAR(20) NULL COMMENT '管道业务等级';",
        "ALTER TABLE `pipe` ADD COLUMN `segment_level` VARCHAR(30) NULL COMMENT '默认行政粒度';",
        "ALTER TABLE `pipe` ADD COLUMN `description` VARCHAR(255) NULL COMMENT '管道说明';",
        "ALTER TABLE `sensor` ADD COLUMN `pipe_segment_id` BIGINT NULL COMMENT '所属管段ID';",
        "ALTER TABLE `task` ADD COLUMN `pipe_segment_id` BIGINT NULL COMMENT '所属管段ID';",
        "DROP TABLE IF EXISTS `pipe_segment`;",
        "DROP TABLE IF EXISTS `pipe_route_node`;",
        "CREATE TABLE `pipe_route_node` (",
        "  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '管道路由节点ID',",
        "  `pipe_id` BIGINT NOT NULL COMMENT '所属管道ID',",
        "  `area_id` BIGINT NOT NULL COMMENT '途经区域ID',",
        "  `node_order` INT NOT NULL COMMENT '路线顺序，从1开始',",
        "  `node_role` VARCHAR(10) NOT NULL COMMENT '节点角色：START/VIA/END',",
        "  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',",
        "  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',",
        "  PRIMARY KEY (`id`),",
        "  UNIQUE KEY `uk_pipe_node_order` (`pipe_id`, `node_order`),",
        "  KEY `idx_pipe_route_node_area` (`area_id`)",
        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管道路由节点表';",
        "CREATE TABLE `pipe_segment` (",
        "  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '管段ID',",
        "  `pipe_id` BIGINT NOT NULL COMMENT '所属管道ID',",
        "  `segment_order` INT NOT NULL COMMENT '管段顺序，从1开始',",
        "  `start_node_id` BIGINT NOT NULL COMMENT '起始路由节点ID',",
        "  `end_node_id` BIGINT NOT NULL COMMENT '结束路由节点ID',",
        "  `start_area_id` BIGINT NOT NULL COMMENT '管段起点区域ID',",
        "  `end_area_id` BIGINT NOT NULL COMMENT '管段终点区域ID',",
        "  `segment_level` VARCHAR(30) NOT NULL COMMENT '管段粒度',",
        "  `name` VARCHAR(100) NOT NULL COMMENT '管段名称',",
        "  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',",
        "  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',",
        "  PRIMARY KEY (`id`),",
        "  UNIQUE KEY `uk_pipe_segment_order` (`pipe_id`, `segment_order`),",
        "  KEY `idx_pipe_segment_start_area` (`start_area_id`),",
        "  KEY `idx_pipe_segment_end_area` (`end_area_id`),",
        "  KEY `idx_pipe_segment_nodes` (`start_node_id`, `end_node_id`)",
        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管段表';",
        "",
    ]


def build_sql(pipes: list[PipeDef], nodes: list[RouteNodeRow], segments: list[SegmentRow], counts: dict[str, int], seed: int, include_schema: bool) -> tuple[str, dict[str, int]]:
    rng = random.Random(seed)
    now = datetime(2026, 4, 22, 9, 0, 0)
    lines: list[str] = [
        "-- Generated by scripts/generate_pipeline_seed.py",
        f"-- Generated at {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}",
        "SET NAMES utf8mb4;",
        "SET FOREIGN_KEY_CHECKS = 0;",
        "",
    ]
    if include_schema:
        lines.extend(schema_sql())
    lines.extend(
        [
            "-- Reset topology-dependent demo rows.",
            "DELETE FROM `inspection`;",
            "DELETE FROM `task`;",
            "DELETE FROM `sensor`;",
            "DELETE FROM `pipe_segment`;",
            "DELETE FROM `pipe_route_node`;",
            "DELETE FROM `pipe`;",
            "",
        ]
    )

    pipe_rows = [
        (
            pipe.id,
            pipe.start_area_id,
            pipe.end_area_id,
            pipe.name,
            now,
            now,
            pipe.pipe_level,
            pipe.segment_level,
            pipe.description,
        )
        for pipe in pipes
    ]
    write_insert(
        lines,
        "pipe",
        ["id", "start_area_id", "end_area_id", "name", "create_time", "update_time", "pipe_level", "segment_level", "description"],
        pipe_rows,
    )

    node_rows = [(row.id, row.pipe_id, row.area_id, row.node_order, row.node_role, now, now) for row in nodes]
    write_insert(lines, "pipe_route_node", ["id", "pipe_id", "area_id", "node_order", "node_role", "create_time", "update_time"], node_rows)

    segment_rows = [
        (
            row.id,
            row.pipe_id,
            row.segment_order,
            row.start_node_id,
            row.end_node_id,
            row.start_area_id,
            row.end_area_id,
            row.segment_level,
            row.name,
            now,
            now,
        )
        for row in segments
    ]
    write_insert(
        lines,
        "pipe_segment",
        [
            "id",
            "pipe_id",
            "segment_order",
            "start_node_id",
            "end_node_id",
            "start_area_id",
            "end_area_id",
            "segment_level",
            "name",
            "create_time",
            "update_time",
        ],
        segment_rows,
    )

    sensor_rows: list[tuple[Any, ...]] = []
    sensor_segment_map: dict[int, SegmentRow] = {}
    for sensor_id in range(1, counts["sensor_count"] + 1):
        segment = segments[(sensor_id - 1) % len(segments)]
        sensor_segment_map[sensor_id] = segment
        created = now - timedelta(days=sensor_id % 180, minutes=sensor_id % 240)
        sensor_rows.append(
            (
                sensor_id,
                choose_area(segment, sensor_id),
                segment.pipe_id,
                segment.id,
                ((sensor_id - 1) % 500) + 1,
                status_for(sensor_id),
                medium_sensor_type(segment.medium, sensor_id),
                created - timedelta(days=30 + sensor_id % 90),
                created,
                created,
            )
        )
    write_insert(
        lines,
        "sensor",
        ["id", "area_id", "pipeline_id", "pipe_segment_id", "repairman_id", "status", "type", "last_overhaul_time", "create_time", "update_time"],
        sensor_rows,
    )

    task_rows: list[tuple[Any, ...]] = []
    for task_id in range(1, counts["task_count"] + 1):
        segment = segments[(task_id * 3 - 1) % len(segments)]
        public_time = now - timedelta(days=task_id % 60, hours=task_id % 24)
        status = task_id % 4
        response_time = public_time + timedelta(hours=1 + task_id % 6) if status >= 1 else None
        accomplish_time = response_time + timedelta(hours=2 + task_id % 10) if status == 2 else None
        task_rows.append(
            (
                task_id,
                None,
                ((task_id - 1) % 500) + 1,
                choose_area(segment, task_id),
                segment.pipe_id,
                segment.id,
                f"{segment.name}巡检处置-{task_id:05d}",
                task_id % 4,
                task_id % 3,
                status,
                public_time,
                response_time,
                accomplish_time,
                "系统生成的管段级演示任务",
                public_time,
                public_time,
            )
        )
    write_insert(
        lines,
        "task",
        [
            "id",
            "inspection_id",
            "repairman_id",
            "area_id",
            "pipe_id",
            "pipe_segment_id",
            "task_name",
            "type",
            "priority",
            "status",
            "public_time",
            "response_time",
            "accomplish_time",
            "feedback_information",
            "create_time",
            "update_time",
        ],
        task_rows,
    )

    inspection_rows: list[tuple[Any, ...]] = []
    for inspection_id in range(1, counts["inspection_count"] + 1):
        sensor_id = ((inspection_id - 1) % counts["sensor_count"]) + 1
        segment = sensor_segment_map[sensor_id]
        pressure, temperature, traffic, shake, data_status = metric_values(segment, inspection_id, rng)
        created = now - timedelta(days=inspection_id % 45, minutes=(inspection_id * 7) % 1440)
        inspection_rows.append((inspection_id, pressure, temperature, traffic, shake, data_status, None, sensor_id, created, created))
    write_insert(
        lines,
        "inspection",
        ["id", "pressure", "temperature", "traffic", "shake", "data_status", "realtime_picture", "sensor_id", "create_time", "update_time"],
        inspection_rows,
    )

    if include_schema:
        lines.extend(
            [
                "ALTER TABLE `pipe` MODIFY `pipe_level` VARCHAR(20) NOT NULL COMMENT '管道业务等级';",
                "ALTER TABLE `pipe` MODIFY `segment_level` VARCHAR(30) NOT NULL COMMENT '默认行政粒度';",
                "ALTER TABLE `sensor` MODIFY `pipe_segment_id` BIGINT NOT NULL COMMENT '所属管段ID';",
                "ALTER TABLE `task` MODIFY `pipe_segment_id` BIGINT NOT NULL COMMENT '所属管段ID';",
                "CREATE INDEX `idx_sensor_segment` ON `sensor` (`pipe_segment_id`);",
                "CREATE INDEX `idx_sensor_pipe_area` ON `sensor` (`pipeline_id`, `area_id`);",
                "CREATE INDEX `idx_task_segment` ON `task` (`pipe_segment_id`);",
                "CREATE INDEX `idx_task_pipe_area_status` ON `task` (`pipe_id`, `area_id`, `status`);",
                "CREATE INDEX `idx_inspection_sensor_time` ON `inspection` (`sensor_id`, `create_time`);",
            ]
        )
    lines.extend(["SET FOREIGN_KEY_CHECKS = 1;", ""])

    summary = {
        "pipe": len(pipes),
        "pipe_route_node": len(nodes),
        "pipe_segment": len(segments),
        "sensor": len(sensor_rows),
        "task": len(task_rows),
        "inspection": len(inspection_rows),
        "empty_sensor_segment": sum(1 for row in sensor_rows if row[3] is None),
        "empty_task_segment": sum(1 for row in task_rows if row[5] is None),
    }
    return "\n".join(lines), summary


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Generate topology-aware pipeline seed SQL.")
    parser.add_argument("--blueprint", type=Path, default=DEFAULT_BLUEPRINT)
    parser.add_argument("--area-sql", type=Path, default=DEFAULT_AREA_SQL)
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    parser.add_argument("--profile", default="sample", choices=["sample", "full"])
    parser.add_argument("--pipe-count", type=int)
    parser.add_argument("--sensor-count", type=int)
    parser.add_argument("--task-count", type=int)
    parser.add_argument("--inspection-count", type=int)
    parser.add_argument("--seed", type=int, default=20260422)
    parser.add_argument("--skip-schema", action="store_true", help="Generate data-only SQL after the topology schema already exists.")
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    pipes, counts = load_blueprint(args.blueprint, args.profile)
    if args.pipe_count is not None:
        counts["pipe_count"] = args.pipe_count
    if args.sensor_count is not None:
        counts["sensor_count"] = args.sensor_count
    if args.task_count is not None:
        counts["task_count"] = args.task_count
    if args.inspection_count is not None:
        counts["inspection_count"] = args.inspection_count

    for key in ["pipe_count", "sensor_count", "task_count", "inspection_count"]:
        if counts[key] <= 0:
            raise ValueError(f"{key} must be greater than 0")

    areas = load_areas(args.area_sql)
    pipes = expand_pipes(pipes, counts["pipe_count"], areas, args.seed)
    nodes, segments = build_topology(pipes)
    sql, summary = build_sql(pipes, nodes, segments, counts, args.seed, include_schema=not args.skip_schema)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(sql, encoding="utf-8")

    print(f"Wrote {args.output}")
    for key, value in summary.items():
        print(f"{key}: {value}")
    if summary["empty_sensor_segment"] or summary["empty_task_segment"]:
        raise RuntimeError("Generated empty pipe_segment_id assignments")


if __name__ == "__main__":
    main()
