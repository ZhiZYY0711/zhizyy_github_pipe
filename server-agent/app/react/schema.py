from typing import Any, Literal

from pydantic import BaseModel, Field, model_validator


ReactActionType = Literal[
    "tool_call",
    "knowledge_search",
    "memory_search",
    "ask_user",
    "final_answer",
]


class ReactSingleAction(BaseModel):
    thought_summary: str = ""
    action: ReactActionType
    tool_name: str | None = None
    tool_input: dict[str, Any] = Field(default_factory=dict)
    search_query: str | None = None
    missing_information: list[str] = Field(default_factory=list)

    @model_validator(mode="after")
    def validate_action_payload(self) -> "ReactSingleAction":
        if self.action == "tool_call" and not self.tool_name:
            raise ValueError("tool_name is required for tool_call")
        if self.action in {"knowledge_search", "memory_search"} and not self.search_query:
            raise ValueError("search_query is required for search actions")
        return self


class ReactAction(BaseModel):
    thought_summary: str = ""
    action: ReactActionType | None = None
    tool_name: str | None = None
    tool_input: dict[str, Any] = Field(default_factory=dict)
    search_query: str | None = None
    missing_information: list[str] = Field(default_factory=list)
    actions: list[ReactSingleAction] = Field(default_factory=list)

    @model_validator(mode="after")
    def validate_action_payload(self) -> "ReactAction":
        if self.actions:
            return self
        if self.action is None:
            raise ValueError("action is required when actions is empty")
        ReactSingleAction(
            thought_summary=self.thought_summary,
            action=self.action,
            tool_name=self.tool_name,
            tool_input=self.tool_input,
            search_query=self.search_query,
            missing_information=self.missing_information,
        )
        return self

    def step_actions(self) -> list[ReactSingleAction]:
        if self.actions:
            return self.actions
        if self.action is None:
            return []
        return [
            ReactSingleAction(
                thought_summary=self.thought_summary,
                action=self.action,
                tool_name=self.tool_name,
                tool_input=self.tool_input,
                search_query=self.search_query,
                missing_information=self.missing_information,
            )
        ]
