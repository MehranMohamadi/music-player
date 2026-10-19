package dev.noor.music.storage

import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.Tag

/** Preserve original multi-value frames when their visible first value was not edited. */
internal fun applyTagFields(tag: Tag, fields: Map<FieldKey, String>) {
    fields.forEach { (key, value) ->
        val original = runCatching { tag.getFirst(key) }.getOrDefault("")
        if (value != original) {
            if (value.isBlank()) tag.deleteField(key) else tag.setField(key, value)
        }
    }
}
