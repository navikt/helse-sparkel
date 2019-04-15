package no.nav.helse.arrow

import arrow.core.Either

// mimicks scala's sequenceU
fun <A, B> List<Either<A, B>>.sequenceU() =
        fold(Either.Right(emptyList<B>()) as Either<A, List<B>>) { acc, either ->
            either.fold({ left ->
                Either.Left(left)
            }, { right ->
                acc.map { list ->
                    list + right
                }
            })
        }
