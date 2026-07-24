package com.github.helltar.anpaside.foundation

import android.content.Context
import androidx.annotation.StringRes

class StringResources(context: Context) {
    private val resources = context.resources

    fun get(@StringRes id: Int, vararg arguments: Any): String =
        // the no-argument overload returns the raw string; the vararg overload always runs
        // String.format, which throws on literal '%' tokens like the '%s' in the code templates
        if (arguments.isEmpty()) resources.getString(id) else resources.getString(id, *arguments)
}

