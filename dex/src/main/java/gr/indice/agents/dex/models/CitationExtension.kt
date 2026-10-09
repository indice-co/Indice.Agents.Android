package gr.indice.agents.dex.models

import android.net.Uri
import gr.indice.agents.network.models.Citation


val String.normalizedUrl: String?
    get() = if (startsWith("http://") || startsWith("https://")) this
    else "https://$this"

val Citation.domain: String? get() {
    if (sourceUrl == null) return null
    return Uri.parse(sourceUrl.normalizedUrl).host?.removePrefix("www.") ?: sourceUrl
}
fun Citation.faviconUrl(): List<String>? {
    val domain = domain ?: return null
    return buildList {
        add("https://t1.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=${Uri.encode("https://$domain")}&size=64")
        add("https://$domain/favicon.ico")
    }
}