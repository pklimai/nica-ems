import react.*
import react.dom.*
import kotlinx.html.js.*
import kotlinx.coroutines.*

private val scope = MainScope()

val app = fc<Props> {
    var config by useState(
        ConfigFile(DBConnectionConfig("",0,"","",""),
            null, false, null, "", emptyList<PageConfig>()  ) )

    useEffectOnce {
        scope.launch {
            config = getConfig()
        }
    }

    h1 {
        + config.title
    }
    ul {
        config.pages.forEach { item ->
            li {
                key = item.name
//                attrs.onClickFunction = {
//                    scope.launch {
//                        deleteShoppingListItem(item)
//                        shoppingList = getShoppingList()
//                    }
//                }
                +"[${item.name}] ${item.api_url} "
            }
        }
    }
//    child(inputComponent) {
//        attrs.onSubmit = { input ->
//            val cartItem = ShoppingListItem(input.replace("!", ""), input.count { it == '!' })
//            scope.launch {
//                addShoppingListItem(cartItem)
//                shoppingList = getShoppingList()
//            }
//        }
//    }

}