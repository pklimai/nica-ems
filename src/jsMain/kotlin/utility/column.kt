package ru.mipt.npm.nica.ems

fun column(field: String, key: String, headerName: String, flex: Int): dynamic {
    val r: dynamic = object {}
    r["field"] = field
    r["key"] = key
    r["headerName"] = headerName
    r["flex"] = flex
    return r
}
