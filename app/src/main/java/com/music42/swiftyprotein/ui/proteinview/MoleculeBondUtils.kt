package com.music42.swiftyprotein.ui.proteinview

import com.music42.swiftyprotein.data.model.Bond

internal fun sameBond(a: Bond, b: Bond): Boolean {
    return (a.atomId1 == b.atomId1 && a.atomId2 == b.atomId2) ||
        (a.atomId1 == b.atomId2 && a.atomId2 == b.atomId1)
}
