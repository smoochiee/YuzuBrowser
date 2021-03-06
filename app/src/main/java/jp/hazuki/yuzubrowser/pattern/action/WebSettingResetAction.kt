/*
 * Copyright (C) 2017 Hazuki
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jp.hazuki.yuzubrowser.pattern.action

import jp.hazuki.yuzubrowser.tab.manager.MainTabData

class WebSettingResetAction(tabData: MainTabData) {
    private val userAgent: String?
    private val javaScriptEnable: Boolean
    private val navLockEnable: Boolean
    private val loadImage: Boolean
    var patternAction: WebSettingPatternAction? = null

    init {
        val settings = tabData.mWebView.settings
        userAgent = settings.userAgentString
        javaScriptEnable = settings.javaScriptEnabled
        navLockEnable = tabData.isNavLock
        loadImage = settings.loadsImagesAutomatically
    }

    fun reset(tab: MainTabData) {
        val settings = tab.mWebView.settings

        settings.userAgentString = userAgent
        settings.javaScriptEnabled = javaScriptEnable
        tab.isNavLock = navLockEnable
        settings.loadsImagesAutomatically = loadImage
    }
}
