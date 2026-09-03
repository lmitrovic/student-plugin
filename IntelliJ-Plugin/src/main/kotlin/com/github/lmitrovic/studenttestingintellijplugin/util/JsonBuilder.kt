package com.github.lmitrovic.studenttestingintellijplugin.util

/**
 * Mali serijalizator bez zavisnosti - dovoljan za ravne JSON objekte koje plugin šalje.
 * Zamena za ručno sklapanje JSON-a string interpolacijom (gde bi navodnik ili `\` u
 * imenu fajla / studentId-u napravio nevalidan JSON).
 */
object JsonBuilder {

    fun obj(vararg pairs: Pair<String, Any?>): String =
        pairs.joinToString(separator = ",", prefix = "{", postfix = "}") { (k, v) ->
            "${quote(k)}:${value(v)}"
        }

    fun value(v: Any?): String = when (v) {
        null -> "null"
        is Boolean, is Number -> v.toString()
        is CharSequence -> quote(v.toString())
        is Map<*, *> -> v.entries.joinToString(separator = ",", prefix = "{", postfix = "}") { (k, vv) ->
            "${quote(k.toString())}:${value(vv)}"
        }
        is Iterable<*> -> v.joinToString(separator = ",", prefix = "[", postfix = "]") { value(it) }
        is Array<*> -> v.joinToString(separator = ",", prefix = "[", postfix = "]") { value(it) }
        else -> quote(v.toString())
    }

    fun quote(s: String): String = buildString(s.length + 2) {
        append('"')
        for (c in s) when {
            c == '"' -> append("\\\"")
            c == '\\' -> append("\\\\")
            c == '\n' -> append("\\n")
            c == '\r' -> append("\\r")
            c == '\t' -> append("\\t")
            c.code < 0x20 -> append("\\u%04x".format(c.code))
            else -> append(c)
        }
        append('"')
    }
}
