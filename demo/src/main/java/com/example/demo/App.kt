package com.example.demo

import android.app.Application
import android.provider.Settings
import com.mobile.vms.VMSMobileSDK
import com.mobile.vms.models.VMSBasicStatic
import com.mobile.vms.models.VMSPermission
import com.mobile.vms.models.VMSStatics
import com.mobile.vms.models.VMSTranslations

class App: Application() {

    companion object {
        // store data here only for example
        var translations: VMSTranslations? = null
        var statics: VMSStatics? = null
        var basicStatic: VMSBasicStatic? = null
        var permissions: List<VMSPermission>? = null
    }

    override fun onCreate() {
        super.onCreate()

        /// baseUrl - input your url
        VMSMobileSDK.Builder(
            application = this,
            baseUrl = "https://xxx.xxx.xxx",
            uuid = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        )
    }
}