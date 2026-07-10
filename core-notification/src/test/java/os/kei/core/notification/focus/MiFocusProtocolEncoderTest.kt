package os.kei.core.notification.focus

import android.app.Application
import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import os.kei.core.notification.R
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class MiFocusProtocolEncoderTest {
    @Test
    fun `local protocol encoder writes V3 param and parcelable registries`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val pendingIntent = PendingIntent.getActivity(
            context,
            912,
            Intent("os.kei.test.LOCAL_FOCUS_ACTION").setPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val bundle = MiFocusProtocolNotification.buildV3 {
            val logoKey = createPicture(
                key = "focus_protocol_logo",
                value = Icon.createWithResource(context, R.drawable.ic_notification_logo),
            )
            val actionKey = createAction(
                key = "focus_protocol_open",
                value = Notification.Action.Builder(
                    Icon.createWithResource(context, R.drawable.ic_notification_logo),
                    "Open",
                    pendingIntent,
                ).build(),
            )
            ticker = "KeiOS"
            business = "keios"
            island {
                islandProperty = 1
                bigIslandArea {
                    imageTextInfoLeft {
                        type = 1
                        picInfo { pic = logoKey }
                    }
                }
            }
            textButton {
                addActionInfo {
                    type = 2
                    action = actionKey
                    actionTitle = "Open"
                    actionBgPressColor = "#1D4ED8"
                    actionBgPressColorDark = "#2563EB"
                    clickWithCollapse = true
                }
            }
        }

        val root = JSONObject(bundle.getString(MI_FOCUS_PARAM_KEY).orEmpty())
        val param = root.getJSONObject(MI_FOCUS_PARAM_V3_KEY)
        assertEquals(1, param.optInt("protocol", -1))
        assertEquals("KeiOS", param.getString("ticker"))
        assertEquals("keios", param.getString("business"))
        assertEquals(
            1,
            param.getJSONObject(MI_FOCUS_ISLAND_KEY).getInt("islandProperty"),
        )
        assertEquals(
            "focus_protocol_logo",
            param
                .getJSONObject(MI_FOCUS_ISLAND_KEY)
                .getJSONObject("bigIslandArea")
                .getJSONObject("imageTextInfoLeft")
                .getJSONObject("picInfo")
                .getString("pic"),
        )
        assertEquals(
            "focus_protocol_open",
            param.getJSONArray("textButton").getJSONObject(0).getString("action"),
        )
        assertEquals(
            "#1D4ED8",
            param.getJSONArray("textButton").getJSONObject(0).getString("actionBgPressColor"),
        )
        assertNotNull(
            bundle.getBundle(MI_FOCUS_PICTURES_KEY)
                ?.getParcelable("focus_protocol_logo", Icon::class.java),
        )
        assertNotNull(
            bundle.getBundle(MI_FOCUS_ACTIONS_KEY)
                ?.getParcelable("focus_protocol_open", Notification.Action::class.java),
        )
    }
}
