package ru.mipt.npm.nica.emd

fun column(field: String, key: String /* TODO check */, headerName: String, flex: Int): dynamic {
    val r: dynamic = object {}
    r["field"] = field
    r["key"] = key
    r["headerName"] = headerName
    r["flex"] = flex
    return r
}
