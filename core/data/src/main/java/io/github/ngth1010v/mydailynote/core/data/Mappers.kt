package io.github.ngth1010v.mydailynote.core.data

import io.github.ngth1010v.mydailynote.core.data.db.EntryEntity
import io.github.ngth1010v.mydailynote.core.data.db.EntryWithGroup
import io.github.ngth1010v.mydailynote.core.data.db.GroupEntity
import io.github.ngth1010v.mydailynote.core.data.model.Entry
import io.github.ngth1010v.mydailynote.core.data.model.EntrySource
import io.github.ngth1010v.mydailynote.core.data.model.Group
import io.github.ngth1010v.mydailynote.core.data.model.TextNormalizer
import java.time.Instant
import java.time.ZoneOffset

internal fun EntryWithGroup.toModel(): Entry = entry.toModel(groupColor, groupIcon)

internal fun EntryEntity.toModel(groupColor: String? = null, groupIcon: String? = null) = Entry(
    id = id,
    text = text,
    description = description,
    achievedAt = Instant.ofEpochMilli(achievedAt),
    zoneOffset = ZoneOffset.ofTotalSeconds(zoneOffsetSeconds),
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt),
    colorOverride = colorOverride,
    iconOverride = iconOverride,
    groupId = groupId,
    source = runCatching { EntrySource.valueOf(source) }.getOrDefault(EntrySource.TEXT),
    groupColor = groupColor,
    groupIcon = groupIcon,
)

internal fun Entry.toEntity(deletedAt: Long? = null) = EntryEntity(
    id = id,
    text = text,
    description = description?.takeIf { it.isNotBlank() },
    achievedAt = achievedAt.toEpochMilli(),
    zoneOffsetSeconds = zoneOffset.totalSeconds,
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt.toEpochMilli(),
    colorOverride = colorOverride,
    iconOverride = iconOverride,
    groupId = groupId,
    source = source.name,
    searchText = TextNormalizer.normalize(text),
    deletedAt = deletedAt,
)

internal fun GroupEntity.toModel() = Group(
    id = id,
    name = name,
    color = color,
    icon = icon,
    description = description,
    createdAt = Instant.ofEpochMilli(createdAt),
)

internal fun Group.toEntity() = GroupEntity(
    id = id,
    name = name,
    color = color,
    icon = icon,
    description = description,
    createdAt = createdAt.toEpochMilli(),
)
