package com.toycs

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class AnimeWakuPlugin: Plugin() {
    override fun load(context: Context) {
        // ลงทะเบียน Provider คลาสหลัก
        registerMainAPI(AnimeWakuProvider())
    }
}