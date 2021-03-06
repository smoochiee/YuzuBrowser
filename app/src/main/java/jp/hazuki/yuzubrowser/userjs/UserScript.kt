package jp.hazuki.yuzubrowser.userjs

import android.os.Parcel
import android.os.Parcelable
import jp.hazuki.yuzubrowser.utils.ErrorReport
import jp.hazuki.yuzubrowser.utils.Logger
import jp.hazuki.yuzubrowser.utils.WebUtils
import jp.hazuki.yuzubrowser.utils.extensions.forEachLine
import java.io.BufferedReader
import java.io.IOException
import java.io.StringReader
import java.util.regex.Pattern

class UserScript : Parcelable {

    val info: UserScriptInfo

    var name: String? = null
    var version: String? = null
    var author: String? = null
    var description: String? = null
    val include = ArrayList<Pattern>(0)
    val exclude = ArrayList<Pattern>(0)
    var isUnwrap: Boolean = false
        private set
    var isRunStart: Boolean = false

    var id: Long
        get() = info.id
        set(id) {
            info.id = id
        }

    var data: String
        get() = info.data
        set(data) {
            info.data = data
            loadHeaderData()
        }

    val runnable: String
        get() = if (isUnwrap) {
            info.data
        } else {
            "(function() {\n${info.data}\n})()"
        }

    var isEnabled: Boolean
        get() = info.isEnabled
        set(enabled) {
            info.isEnabled = enabled
        }

    constructor() {
        info = UserScriptInfo()
    }

    constructor(id: Long, data: String, enabled: Boolean) {
        info = UserScriptInfo(id, data, enabled)
        loadHeaderData()
    }

    constructor(data: String) {
        info = UserScriptInfo(data)
        loadHeaderData()
    }

    constructor(info: UserScriptInfo) {
        this.info = info
        loadHeaderData()
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeLong(info.id)
        dest.writeString(info.data)
        dest.writeInt(if (info.isEnabled) 1 else 0)
    }

    constructor(source: Parcel) {
        val id = source.readLong()
        val data = source.readString()
        val enabled = source.readInt() == 1
        info = UserScriptInfo(id, data, enabled)
        loadHeaderData()
    }

    private fun loadHeaderData() {
        name = null
        version = null
        description = null
        include.clear()
        exclude.clear()

        try {
            val reader = BufferedReader(StringReader(info.data))

            if (reader.readLine()?.let { sHeaderStartPattern.matcher(it).matches() } != true) {
                Logger.w(TAG, "Header (start) parser error")
                return
            }

            reader.forEachLine { line ->
                val matcher = sHeaderMainPattern.matcher(line)
                if (!matcher.matches()) {
                    if (sHeaderEndPattern.matcher(line).matches()) {
                        return
                    }
                    Logger.w(TAG, "Unknown header : " + line)
                } else {
                    val field = matcher.group(1)
                    val value = matcher.group(2)
                    readData(field, value, line)
                }
            }

            Logger.w(TAG, "Header (end) parser error")
        } catch (e: IOException) {
            ErrorReport.printAndWriteLog(e)
        }

    }

    private fun readData(field: String?, value: String?, line: String) {
        if ("name".equals(field, ignoreCase = true)) {
            name = value
        } else if ("version".equals(field, ignoreCase = true)) {
            version = value
        } else if ("author".equals(field, ignoreCase = true)) {
            author = value
        } else if ("description".equals(field, ignoreCase = true)) {
            description = value
        } else if ("include".equals(field, ignoreCase = true)) {
            WebUtils.makeUrlPattern(value)?.let {
                include.add(it)
            }
        } else if ("exclude".equals(field, ignoreCase = true)) {
            WebUtils.makeUrlPattern(value)?.let {
                exclude.add(it)
            }
        } else if ("unwrap".equals(field, ignoreCase = true)) {
            isUnwrap = true
        } else if ("run-at".equals(field, ignoreCase = true)) {
            isRunStart = "document-start".equals(value, ignoreCase = true)
        } else if ("match".equals(field, ignoreCase = true) && value != null) {
            val patternUrl = "?^" + value.replace("?", "\\?").replace(".", "\\.")
                    .replace("*", ".*").replace("+", ".+")
                    .replace("://.*\\.", "://((?![\\./]).)*\\.").replace("^\\.\\*://".toRegex(), "https?://")
            WebUtils.makeUrlPattern(patternUrl)?.let {
                include.add(it)
            }
        } else {
            Logger.w(TAG, "Unknown header : " + line)
        }
    }

    companion object {
        private const val TAG = "UserScript"

        @JvmField
        val CREATOR: Parcelable.Creator<UserScript> = object : Parcelable.Creator<UserScript> {
            override fun createFromParcel(source: Parcel): UserScript = UserScript(source)

            override fun newArray(size: Int): Array<UserScript?> = arrayOfNulls(size)
        }

        private val sHeaderStartPattern = Pattern.compile("\\s*//\\s*==UserScript==\\s*", Pattern.CASE_INSENSITIVE)
        private val sHeaderEndPattern = Pattern.compile("\\s*//\\s*==/UserScript==\\s*", Pattern.CASE_INSENSITIVE)
        private val sHeaderMainPattern = Pattern.compile("\\s*//\\s*@(\\S+)(?:\\s+(.*))?", Pattern.CASE_INSENSITIVE)
    }
}
