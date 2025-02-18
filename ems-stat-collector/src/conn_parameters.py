from dataclasses import dataclass

@dataclass
class ConnParameters:
    host: str
    port: int
    db_name: str
    user: str
    password: str

    def from_dict(dict: dict):
        template = {}
        fields = ConnParameters.__dataclass_fields__.keys()
        for field in fields:
            value = dict.get(field, None)
            if value is None:
                raise ValueError(f"Value \"{field}\" is not present in the config")
            if type(value) != (expected_type := ConnParameters.__dataclass_fields__[field].type):
                raise ValueError(f"Expected \"{field}\" to be {expected_type}, but got {type(value)}")
            template[field] = value

        return ConnParameters(**template)
