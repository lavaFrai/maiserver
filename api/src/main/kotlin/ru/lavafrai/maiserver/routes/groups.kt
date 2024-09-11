package ru.lavafrai.maiserver.routes

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.lavafrai.mai.api.models.group.Group
import ru.lavafrai.maiserver.cache.Cache
import ru.lavafrai.maiserver.cache.CacheKeys
import ru.lavafrai.maiserver.metrics.MetricName
import ru.lavafrai.maiserver.metrics.Metrics
import ru.lavafrai.maiserver.parser.Parser
import java.time.LocalDateTime

fun Route.groups() {
    route("/groups") {
        val cache = Cache.getInstance()
        val parser = Parser.getInstance()

        get {
            Metrics.getInstance().incrementMetric(MetricName.GROUPS_LIST_GET)

            call.respond(
                Json.encodeToString(
                    cache.getOrExecuteAndCache<List<Group>>(CacheKeys.GROUPS_LIST, LocalDateTime.now().plusDays(14)) {
                        parser.parseGroupsListOrException()
                    }
                )
            )
        }
    }
}
