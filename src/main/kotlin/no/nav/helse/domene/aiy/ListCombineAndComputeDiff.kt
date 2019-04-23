package no.nav.helse.domene.aiy

internal fun <T, S> List<T>.combineAndComputeDiff(other: List<S>, matcher: (T, S) -> Boolean): Triple<Map<T, List<S>>, List<T>, List<S>> {
    val destination = mutableMapOf<T, List<S>>()
    val emptyOrDuplicateElements = mutableListOf<T>()
    var otherElements = other

    for (element in this) {
        otherElements.partition { otherElement ->
            matcher(element, otherElement)
        }.let { (matches, noMatches) ->
            otherElements = noMatches

            if (matches.isEmpty() || destination[element] != null) {
                emptyOrDuplicateElements.add(element)
            } else {
                destination[element] = matches.toList()
            }
        }
    }

    return Triple(
            first = destination,
            second = emptyOrDuplicateElements,
            third = otherElements)
}
