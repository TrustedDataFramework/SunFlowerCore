package org.tdf.sunflower.controller

import org.tdf.sunflower.types.PageSize

data class PoolQuery(override var page: Int = 0, override var size: Int = 0, var status: String = "") : PageSize