package com.music42.swiftyprotein.ui.proteinlist

import com.music42.swiftyprotein.data.repository.LigandRepository

internal fun formatLigandSubtitle(info: LigandRepository.LigandCacheInfo?): String? {
    return info?.let {
        buildString {
            if (it.formula.isNotBlank()) append(it.formula)
            if (it.atomCount > 0) {
                if (isNotEmpty()) append(" · ")
                append("${it.atomCount} atoms")
            }
        }.ifEmpty { null }
    }
}
