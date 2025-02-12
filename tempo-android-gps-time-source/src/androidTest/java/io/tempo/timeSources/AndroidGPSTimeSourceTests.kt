/*
 * Copyright 2017 Allan Yoshio Hasegawa
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

package io.tempo.timeSources

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.location.LocationProvider
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import kotlinx.coroutines.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class AndroidGPSTimeSourceTests {
    private val context = InstrumentationRegistry.getInstrumentation().context

    @Test
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    fun testNormalPath() {
        // On a device, go to "Developer Options" and enable location mocks for this library.
        // Also, manually open the app in the "Settings" screen and enable the "Location" permission.
        val gpsUptime = 50L
        val gpsTime = 100L
        val gpsElapsedRealTime = 200L

        // Setup location mock
        val mgr = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val provider = LocationManager.GPS_PROVIDER
        try {
            mgr.addTestProvider(provider, false, true, false, false, true, true, true, 0, 0)
        } catch (_: Throwable) {

        }
        mgr.setTestProviderEnabled(provider, true)
        mgr.setTestProviderStatus(provider, LocationProvider.AVAILABLE, null, gpsUptime)
        val location = Location(provider)
            .apply {
                latitude = 1.0
                longitude = 2.0
                accuracy = 3.0f
                bearing = 4.0f
                speed = 5.0f
                elapsedRealtimeNanos = gpsElapsedRealTime
                extras = null
                time = gpsTime
            }

        GlobalScope.launch {
            delay(100L)
            mgr.setTestProviderLocation(provider, location)
        }

        val time = runBlocking {
            withTimeout(10_000L) {
                AndroidGPSTimeSource(context).requestTime()
            }
        }

        assertThat(time, equalTo(gpsTime))
    }
}