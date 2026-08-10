package com.example.data

import androidx.compose.runtime.mutableStateListOf
import kotlin.reflect.KProperty

class NavigationStackState(initial: String) {
    private val stack = mutableStateListOf(initial)

    val canGoBack: Boolean
        get() = stack.size > 1

    fun goBack() {
        if (stack.size > 1) {
            stack.removeAt(stack.lastIndex)
        }
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): String = stack.last()

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
        if (stack.lastOrNull() != value) {
            stack.add(value)
        }
    }
}
