@file:JsModule("@mui/x-data-grid")
@file:JsNonModule

package mui.x

// https://github.com/mui-org/material-ui-x/blob/master/packages/grid/x-data-grid/src/DataGrid.tsx

external interface DataGridProps: mui.system.StandardProps, react.PropsWithChildren {
    var columns: dynamic // Array<Map<String, Any>>
    var rows: dynamic // Array<Map<String, Any>>?
    var pageSize: dynamic
    var rowsPerPageOptions: dynamic
    var columnBuffer: dynamic
}

// @JsName("default")
external val DataGrid: react.FC<DataGridProps>
