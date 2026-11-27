package com.saurav.pixelmusic.utils.shapes

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

enum class FluidBlobType {
    BLOB_TOP_LEFT,
    BLOB_TOP_RIGHT,
    BLOB_CENTER,
    BLOB_RIGHT_CIRCLE,
    BLOB_BOTTOM_LEFT,
    COOKIE_BOTTOM_RIGHT,
}

class FluidBlobShape(private val type: FluidBlobType) : Shape {
    private var cachedWidth: Float = -1f
    private var cachedHeight: Float = -1f
    private var cachedOutline: Outline? = null

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val currentOutline = cachedOutline
        if (size.width == cachedWidth && size.height == cachedHeight && currentOutline != null) {
            return currentOutline
        }

        val w = size.width
        val h = size.height
        val path = Path().apply {
            when (type) {
                FluidBlobType.BLOB_TOP_LEFT -> {
                    moveTo(0.2247f * w, 0.9988f * h)
                    cubicTo(0.2025f * w, 1.0007f * h, 0.1765f * w, 0.9957f * h, 0.1580f * w, 0.9893f * h)
                    cubicTo(0.1395f * w, 0.9829f * h, 0.1254f * w, 0.9701f * h, 0.1136f * w, 0.9608f * h)
                    cubicTo(0.1017f * w, 0.9515f * h, 0.0985f * w, 0.9499f * h, 0.0877f * w, 0.9335f * h)
                    cubicTo(0.0768f * w, 0.9171f * h, 0.0605f * w, 0.8950f * h, 0.0481f * w, 0.8622f * h)
                    cubicTo(0.0358f * w, 0.8295f * h, 0.0217f * w, 0.7803f * h, 0.0136f * w, 0.7363f * h)
                    cubicTo(0.0054f * w, 0.6924f * h, 0.0000f * w, 0.6561f * h, -0.0012f * w, 0.5986f * h)
                    cubicTo(-0.0025f * w, 0.5411f * h, 0.0005f * w, 0.4477f * h, 0.0062f * w, 0.3919f * h)
                    cubicTo(0.0119f * w, 0.3361f * h, 0.0217f * w, 0.3000f * h, 0.0333f * w, 0.2637f * h)
                    cubicTo(0.0449f * w, 0.2273f * h, 0.0590f * w, 0.1990f * h, 0.0753f * w, 0.1734f * h)
                    cubicTo(0.0916f * w, 0.1477f * h, 0.1114f * w, 0.1278f * h, 0.1309f * w, 0.1105f * h)
                    cubicTo(0.1504f * w, 0.0931f * h, 0.1704f * w, 0.0815f * h, 0.1926f * w, 0.0701f * h)
                    cubicTo(0.2148f * w, 0.0587f * h, 0.2328f * w, 0.0506f * h, 0.2642f * w, 0.0416f * h)
                    cubicTo(0.2956f * w, 0.0325f * h, 0.3321f * w, 0.0226f * h, 0.3802f * w, 0.0154f * h)
                    cubicTo(0.4284f * w, 0.0083f * h, 0.5067f * w, 0.0012f * h, 0.5531f * w, -0.0012f * h)
                    cubicTo(0.5995f * w, -0.0036f * h, 0.6247f * w, -0.0024f * h, 0.6593f * w, 0.0012f * h)
                    cubicTo(0.6938f * w, 0.0048f * h, 0.7284f * w, 0.0102f * h, 0.7605f * w, 0.0202f * h)
                    cubicTo(0.7926f * w, 0.0302f * h, 0.8227f * w, 0.0423f * h, 0.8519f * w, 0.0606f * h)
                    cubicTo(0.8810f * w, 0.0789f * h, 0.9126f * w, 0.1055f * h, 0.9346f * w, 0.1306f * h)
                    cubicTo(0.9565f * w, 0.1558f * h, 0.9733f * w, 0.1884f * h, 0.9840f * w, 0.2114f * h)
                    cubicTo(0.9946f * w, 0.2344f * h, 0.9963f * w, 0.2506f * h, 0.9988f * w, 0.2684f * h)
                    cubicTo(1.0012f * w, 0.2862f * h, 1.0037f * w, 0.2976f * h, 0.9988f * w, 0.3183f * h)
                    cubicTo(0.9938f * w, 0.3390f * h, 0.9904f * w, 0.3644f * h, 0.9691f * w, 0.3919f * h)
                    cubicTo(0.9479f * w, 0.4195f * h, 0.9165f * w, 0.4518f * h, 0.8716f * w, 0.4834f * h)
                    cubicTo(0.8267f * w, 0.5150f * h, 0.7383f * w, 0.5565f * h, 0.6988f * w, 0.5808f * h)
                    cubicTo(0.6593f * w, 0.6050f * h, 0.6573f * w, 0.6083f * h, 0.6346f * w, 0.6283f * h)
                    cubicTo(0.6119f * w, 0.6482f * h, 0.5960f * w, 0.6618f * h, 0.5617f * w, 0.7007f * h)
                    cubicTo(0.5274f * w, 0.7397f * h, 0.4620f * w, 0.8240f * h, 0.4284f * w, 0.8622f * h)
                    cubicTo(0.3948f * w, 0.9005f * h, 0.3832f * w, 0.9107f * h, 0.3605f * w, 0.9299f * h)
                    cubicTo(0.3378f * w, 0.9492f * h, 0.3141f * w, 0.9660f * h, 0.2914f * w, 0.9774f * h)
                    cubicTo(0.2686f * w, 0.9888f * h, 0.2469f * w, 0.9969f * h, 0.2247f * w, 0.9988f * h)
                    close()
                }
                FluidBlobType.BLOB_TOP_RIGHT -> {
                    moveTo(0.7045f * w, 0.9985f * h)
                    cubicTo(0.6860f * w, 1.0009f * h, 0.6802f * w, 1.0015f * h, 0.6591f * w, 0.9956f * h)
                    cubicTo(0.6380f * w, 0.9898f * h, 0.6068f * w, 0.9808f * h, 0.5779f * w, 0.9636f * h)
                    cubicTo(0.5490f * w, 0.9464f * h, 0.5159f * w, 0.9207f * h, 0.4854f * w, 0.8921f * h)
                    cubicTo(0.4549f * w, 0.8636f * h, 0.4195f * w, 0.8181f * h, 0.3945f * w, 0.7930f * h)
                    cubicTo(0.3695f * w, 0.7680f * h, 0.3578f * w, 0.7574f * h, 0.3344f * w, 0.7420f * h)
                    cubicTo(0.3110f * w, 0.7265f * h, 0.2971f * w, 0.7172f * h, 0.2532f * w, 0.7012f * h)
                    cubicTo(0.2094f * w, 0.6851f * h, 0.1200f * w, 0.6650f * h, 0.0800f * w, 0.6400f * h)
                    cubicTo(0.0350f * w, 0.6100f * h, 0.0050f * w, 0.5600f * h, 0.0080f * w, 0.5100f * h)
                    cubicTo(0.0120f * w, 0.4400f * h, 0.0450f * w, 0.3800f * h, 0.0420f * w, 0.3300f * h)
                    cubicTo(0.0380f * w, 0.2700f * h, 0.0220f * w, 0.2400f * h, 0.0240f * w, 0.1900f * h)
                    cubicTo(0.0260f * w, 0.1600f * h, 0.0400f * w, 0.1380f * h, 0.0568f * w, 0.1254f * h)
                    cubicTo(0.0695f * w, 0.1102f * h, 0.0825f * w, 0.0974f * h, 0.1039f * w, 0.0831f * h)
                    cubicTo(0.1253f * w, 0.0688f * h, 0.1584f * w, 0.0510f * h, 0.1851f * w, 0.0394f * h)
                    cubicTo(0.2117f * w, 0.0277f * h, 0.2321f * w, 0.0198f * h, 0.2630f * w, 0.0131f * h)
                    cubicTo(0.2938f * w, 0.0064f * h, 0.3338f * w, 0.0000f * h, 0.3701f * w, -0.0015f * h)
                    cubicTo(0.4065f * w, -0.0029f * h, 0.4442f * w, -0.0015f * h, 0.4805f * w, 0.0044f * h)
                    cubicTo(0.5169f * w, 0.0102f * h, 0.5529f * w, 0.0204f * h, 0.5877f * w, 0.0335f * h)
                    cubicTo(0.6224f * w, 0.0466f * h, 0.6565f * w, 0.0647f * h, 0.6883f * w, 0.0831f * h)
                    cubicTo(0.7201f * w, 0.1015f * h, 0.7497f * w, 0.1207f * h, 0.7792f * w, 0.1443f * h)
                    cubicTo(0.8088f * w, 0.1679f * h, 0.8380f * w, 0.1927f * h, 0.8653f * w, 0.2245f * h)
                    cubicTo(0.8925f * w, 0.2563f * h, 0.9237f * w, 0.3003f * h, 0.9432f * w, 0.3353f * h)
                    cubicTo(0.9627f * w, 0.3703f * h, 0.9731f * w, 0.3994f * h, 0.9821f * w, 0.4344f * h)
                    cubicTo(0.9912f * w, 0.4694f * h, 0.9974f * w, 0.5102f * h, 0.9984f * w, 0.5452f * h)
                    cubicTo(0.9994f * w, 0.5802f * h, 0.9935f * w, 0.6157f * h, 0.9886f * w, 0.6443f * h)
                    cubicTo(0.9838f * w, 0.6729f * h, 0.9805f * w, 0.6875f * h, 0.9692f * w, 0.7172f * h)
                    cubicTo(0.9578f * w, 0.7470f * h, 0.9409f * w, 0.7878f * h, 0.9205f * w, 0.8222f * h)
                    cubicTo(0.9000f * w, 0.8566f * h, 0.8708f * w, 0.8977f * h, 0.8458f * w, 0.9242f * h)
                    cubicTo(0.8208f * w, 0.9507f * h, 0.7929f * w, 0.9685f * h, 0.7695f * w, 0.9810f * h)
                    cubicTo(0.7461f * w, 0.9936f * h, 0.7231f * w, 0.9962f * h, 0.7045f * w, 0.9985f * h)
                    close()
                }
                FluidBlobType.BLOB_CENTER -> {
                    moveTo(0.3858f * w, 0.9987f * h)
                    cubicTo(0.3556f * w, 1.0008f * h, 0.3236f * w, 0.9979f * h, 0.2966f * w, 0.9934f * h)
                    cubicTo(0.2696f * w, 0.9889f * h, 0.2488f * w, 0.9836f * h, 0.2231f * w, 0.9723f * h)
                    cubicTo(0.1974f * w, 0.9610f * h, 0.1690f * w, 0.9475f * h, 0.1417f * w, 0.9248f * h)
                    cubicTo(0.1144f * w, 0.9021f * h, 0.0790f * w, 0.8625f * h, 0.0591f * w, 0.8364f * h)
                    cubicTo(0.0391f * w, 0.8103f * h, 0.0320f * w, 0.7921f * h, 0.0223f * w, 0.7678f * h)
                    cubicTo(0.0126f * w, 0.7435f * h, 0.0052f * w, 0.7190f * h, 0.0013f * w, 0.6913f * h)
                    cubicTo(-0.0026f * w, 0.6636f * h, -0.0047f * w, 0.6325f * h, -0.0013f * w, 0.6016f * h)
                    cubicTo(0.0021f * w, 0.5707f * h, 0.0084f * w, 0.5388f * h, 0.0223f * w, 0.5066f * h)
                    cubicTo(0.0362f * w, 0.4744f * h, 0.0493f * w, 0.4485f * h, 0.0827f * w, 0.4090f * h)
                    cubicTo(0.1160f * w, 0.3694f * h, 0.1969f * w, 0.3047f * h, 0.2218f * w, 0.2691f * h)
                    cubicTo(0.2467f * w, 0.2335f * h, 0.2265f * w, 0.2169f * h, 0.2323f * w, 0.1953f * h)
                    cubicTo(0.2381f * w, 0.1736f * h, 0.2467f * w, 0.1562f * h, 0.2559f * w, 0.1398f * h)
                    cubicTo(0.2651f * w, 0.1235f * h, 0.2745f * w, 0.1111f * h, 0.2874f * w, 0.0976f * h)
                    cubicTo(0.3003f * w, 0.0842f * h, 0.3144f * w, 0.0702f * h, 0.3333f * w, 0.0594f * h)
                    cubicTo(0.3522f * w, 0.0485f * h, 0.3745f * w, 0.0383f * h, 0.4016f * w, 0.0330f * h)
                    cubicTo(0.4286f * w, 0.0277f * h, 0.4627f * w, 0.0330f * h, 0.4961f * w, 0.0277f * h)
                    cubicTo(0.5294f * w, 0.0224f * h, 0.5682f * w, 0.0058f * h, 0.6010f * w, 0.0013f * h)
                    cubicTo(0.6339f * w, -0.0032f * h, 0.6661f * w, -0.0013f * h, 0.6929f * w, 0.0013f * h)
                    cubicTo(0.7197f * w, 0.0040f * h, 0.7362f * w, 0.0074f * h, 0.7612f * w, 0.0172f * h)
                    cubicTo(0.7861f * w, 0.0269f * h, 0.8157f * w, 0.0398f * h, 0.8425f * w, 0.0594f * h)
                    cubicTo(0.8693f * w, 0.0789f * h, 0.9005f * w, 0.1071f * h, 0.9226f * w, 0.1346f * h)
                    cubicTo(0.9446f * w, 0.1620f * h, 0.9625f * w, 0.1934f * h, 0.9751f * w, 0.2243f * h)
                    cubicTo(0.9877f * w, 0.2551f * h, 0.9948f * w, 0.2902f * h, 0.9987f * w, 0.3193f * h)
                    cubicTo(1.0026f * w, 0.3483f * h, 1.0008f * w, 0.3747f * h, 0.9987f * w, 0.3984f * h)
                    cubicTo(0.9966f * w, 0.4222f * h, 0.9934f * w, 0.4380f * h, 0.9856f * w, 0.4617f * h)
                    cubicTo(0.9777f * w, 0.4855f * h, 0.9633f * w, 0.5190f * h, 0.9514f * w, 0.5409f * h)
                    cubicTo(0.9380f * w, 0.5700f * h, 0.9150f * w, 0.6100f * h, 0.8937f * w, 0.6728f * h)
                    cubicTo(0.8850f * w, 0.7077f * h, 0.8785f * w, 0.7770f * h, 0.8675f * w, 0.8100f * h)
                    cubicTo(0.8564f * w, 0.8430f * h, 0.8457f * w, 0.8528f * h, 0.8281f * w, 0.8707f * h)
                    cubicTo(0.8105f * w, 0.8887f * h, 0.7853f * w, 0.9066f * h, 0.7612f * w, 0.9169f * h)
                    cubicTo(0.7370f * w, 0.9272f * h, 0.7118f * w, 0.9319f * h, 0.6824f * w, 0.9327f * h)
                    cubicTo(0.6500f * w, 0.9340f * h, 0.5600f * w, 0.9450f * h, 0.4777f * w, 0.9802f * h)
                    cubicTo(0.4462f * w, 0.9916f * h, 0.4160f * w, 0.9966f * h, 0.3858f * w, 0.9987f * h)
                    close()
                }
                FluidBlobType.BLOB_RIGHT_CIRCLE -> {
                    moveTo(0.5333f * w, 0.9972f * h)
                    cubicTo(0.4622f * w, 0.9944f * h, 0.2450f * w, 0.9765f * h, 0.1667f * w, 0.9581f * h)
                    cubicTo(0.0883f * w, 0.9397f * h, 0.0922f * w, 0.9263f * h, 0.0639f * w, 0.8883f * h)
                    cubicTo(0.0356f * w, 0.8503f * h, -0.0011f * w, 0.8145f * h, -0.0028f * w, 0.7318f * h)
                    cubicTo(-0.0044f * w, 0.6492f * h, 0.0306f * w, 0.4788f * h, 0.0528f * w, 0.3911f * h)
                    cubicTo(0.0750f * w, 0.3034f * h, 0.1083f * w, 0.2497f * h, 0.1306f * w, 0.2067f * h)
                    cubicTo(0.1528f * w, 0.1637f * h, 0.1622f * w, 0.1575f * h, 0.1861f * w, 0.1341f * h)
                    cubicTo(0.2100f * w, 0.1106f * h, 0.2244f * w, 0.0872f * h, 0.2722f * w, 0.0642f * h)
                    cubicTo(0.3200f * w, 0.0413f * h, 0.4044f * w, 0.0045f * h, 0.4722f * w, -0.0028f * h)
                    cubicTo(0.5400f * w, -0.0101f * h, 0.6178f * w, 0.0017f * h, 0.6778f * w, 0.0196f * h)
                    cubicTo(0.7378f * w, 0.0374f * h, 0.7939f * w, 0.0749f * h, 0.8333f * w, 0.1034f * h)
                    cubicTo(0.8728f * w, 0.1318f * h, 0.8922f * w, 0.1598f * h, 0.9139f * w, 0.1899f * h)
                    cubicTo(0.9356f * w, 0.2201f * h, 0.9500f * w, 0.2391f * h, 0.9639f * w, 0.2849f * h)
                    cubicTo(0.9778f * w, 0.3307f * h, 0.9956f * w, 0.3939f * h, 0.9972f * w, 0.4637f * h)
                    cubicTo(0.9989f * w, 0.5335f * h, 0.9906f * w, 0.6385f * h, 0.9750f * w, 0.7039f * h)
                    cubicTo(0.9594f * w, 0.7693f * h, 0.9233f * w, 0.8207f * h, 0.9028f * w, 0.8547f * h)
                    cubicTo(0.8822f * w, 0.8888f * h, 0.8783f * w, 0.8888f * h, 0.8500f * w, 0.9078f * h)
                    cubicTo(0.8217f * w, 0.9268f * h, 0.7639f * w, 0.9564f * h, 0.7333f * w, 0.9693f * h)
                    cubicTo(0.7028f * w, 0.9821f * h, 0.6700f * w, 0.9880f * h, 0.6300f * w, 0.9930f * h)
                    cubicTo(0.5900f * w, 0.9980f * h, 0.5600f * w, 0.9975f * h, 0.5333f * w, 0.9972f * h)
                    close()
                }
                FluidBlobType.BLOB_BOTTOM_LEFT -> {
                    moveTo(0.3490f * w, 0.9986f * h)
                    cubicTo(0.3224f * w, 1.0009f * h, 0.2904f * w, 1.0006f * h, 0.2682f * w, 0.9986f * h)
                    cubicTo(0.2461f * w, 0.9966f * h, 0.2352f * w, 0.9937f * h, 0.2161f * w, 0.9871f * h)
                    cubicTo(0.1971f * w, 0.9805f * h, 0.1698f * w, 0.9676f * h, 0.1536f * w, 0.9585f * h)
                    cubicTo(0.1375f * w, 0.9493f * h, 0.1365f * w, 0.9516f * h, 0.1198f * w, 0.9327f * h)
                    cubicTo(0.1031f * w, 0.9138f * h, 0.0688f * w, 0.8700f * h, 0.0534f * w, 0.8453f * h)
                    cubicTo(0.0380f * w, 0.8206f * h, 0.0357f * w, 0.8117f * h, 0.0273f * w, 0.7851f * h)
                    cubicTo(0.0190f * w, 0.7585f * h, 0.0086f * w, 0.7198f * h, 0.0039f * w, 0.6848f * h)
                    cubicTo(-0.0008f * w, 0.6499f * h, -0.0013f * w, 0.6040f * h, -0.0013f * w, 0.5759f * h)
                    cubicTo(-0.0013f * w, 0.5479f * h, -0.0021f * w, 0.5481f * h, 0.0039f * w, 0.5158f * h)
                    cubicTo(0.0099f * w, 0.4834f * h, 0.0260f * w, 0.4332f * h, 0.0352f * w, 0.3811f * h)
                    cubicTo(0.0443f * w, 0.3289f * h, 0.0516f * w, 0.2456f * h, 0.0586f * w, 0.2034f * h)
                    cubicTo(0.0656f * w, 0.1613f * h, 0.0661f * w, 0.1544f * h, 0.0768f * w, 0.1289f * h)
                    cubicTo(0.0875f * w, 0.1034f * h, 0.1065f * w, 0.0693f * h, 0.1224f * w, 0.0501f * h)
                    cubicTo(0.1383f * w, 0.0309f * h, 0.1557f * w, 0.0215f * h, 0.1719f * w, 0.0129f * h)
                    cubicTo(0.1880f * w, 0.0043f * h, 0.2026f * w, 0.0009f * h, 0.2188f * w, -0.0014f * h)
                    cubicTo(0.2349f * w, -0.0037f * h, 0.2500f * w, -0.0052f * h, 0.2682f * w, -0.0014f * h)
                    cubicTo(0.2865f * w, 0.0023f * h, 0.3091f * w, 0.0109f * h, 0.3281f * w, 0.0215f * h)
                    cubicTo(0.3471f * w, 0.0321f * h, 0.3534f * w, 0.0344f * h, 0.3828f * w, 0.0616f * h)
                    cubicTo(0.4122f * w, 0.0888f * h, 0.4732f * w, 0.1553f * h, 0.5052f * w, 0.1848f * h)
                    cubicTo(0.5372f * w, 0.2143f * h, 0.5487f * w, 0.2212f * h, 0.5755f * w, 0.2393f * h)
                    cubicTo(0.6023f * w, 0.2573f * h, 0.6294f * w, 0.2751f * h, 0.6667f * w, 0.2937f * h)
                    cubicTo(0.7039f * w, 0.3123f * h, 0.7600f * w, 0.3309f * h, 0.7995f * w, 0.3510f * h)
                    cubicTo(0.8391f * w, 0.3711f * h, 0.8786f * w, 0.3960f * h, 0.9036f * w, 0.4140f * h)
                    cubicTo(0.9286f * w, 0.4321f * h, 0.9372f * w, 0.4444f * h, 0.9492f * w, 0.4585f * h)
                    cubicTo(0.9612f * w, 0.4725f * h, 0.9669f * w, 0.4791f * h, 0.9753f * w, 0.4986f * h)
                    cubicTo(0.9836f * w, 0.5181f * h, 0.9961f * w, 0.5516f * h, 0.9987f * w, 0.5759f * h)
                    cubicTo(1.0013f * w, 0.6003f * h, 0.9948f * w, 0.6275f * h, 0.9909f * w, 0.6447f * h)
                    cubicTo(0.9870f * w, 0.6619f * h, 0.9854f * w, 0.6642f * h, 0.9753f * w, 0.6791f * h)
                    cubicTo(0.9651f * w, 0.6940f * h, 0.9500f * w, 0.7180f * h, 0.9300f * w, 0.7380f * h)
                    cubicTo(0.8900f * w, 0.7800f * h, 0.8100f * w, 0.8300f * h, 0.7200f * w, 0.8800f * h)
                    cubicTo(0.6200f * w, 0.9350f * h, 0.5100f * w, 0.9700f * h, 0.4200f * w, 0.9900f * h)
                    cubicTo(0.3800f * w, 0.9980f * h, 0.3600f * w, 0.9985f * h, 0.3490f * w, 0.9986f * h)
                    close()
                }
                FluidBlobType.COOKIE_BOTTOM_RIGHT -> {
                    moveTo(0.9266f * w, 0.5000f * h)
                    cubicTo(0.9401f * w, 0.5319f * h, 0.9633f * w, 0.5740f * h, 0.9561f * w, 0.6051f * h)
                    cubicTo(0.9489f * w, 0.6362f * h, 0.9101f * w, 0.6638f * h, 0.8844f * w, 0.6864f * h)
                    cubicTo(0.8582f * w, 0.7094f * h, 0.8207f * w, 0.7170f * h, 0.8008f * w, 0.7421f * h)
                    cubicTo(0.7814f * w, 0.7672f * h, 0.7823f * w, 0.8055f * h, 0.7658f * w, 0.8362f * h)
                    cubicTo(0.7494f * w, 0.8668f * h, 0.7316f * w, 0.9111f * h, 0.7030f * w, 0.9251f * h)
                    cubicTo(0.6747f * w, 0.9391f * h, 0.6287f * w, 0.9255f * h, 0.5949f * w, 0.9191f * h)
                    cubicTo(0.5612f * w, 0.9132f * h, 0.5316f * w, 0.8881f * h, 0.5000f * w, 0.8881f * h)
                    cubicTo(0.4684f * w, 0.8881f * h, 0.4388f * w, 0.9132f * h, 0.4051f * w, 0.9191f * h)
                    cubicTo(0.3713f * w, 0.9255f * h, 0.3253f * w, 0.9391f * h, 0.2970f * w, 0.9251f * h)
                    cubicTo(0.2684f * w, 0.9111f * h, 0.2506f * w, 0.8668f * h, 0.2342f * w, 0.8362f * h)
                    cubicTo(0.2177f * w, 0.8055f * h, 0.2186f * w, 0.7672f * h, 0.1992f * w, 0.7421f * h)
                    cubicTo(0.1793f * w, 0.7170f * h, 0.1418f * w, 0.7094f * h, 0.1156f * w, 0.6864f * h)
                    cubicTo(0.0899f * w, 0.6638f * h, 0.0511f * w, 0.6362f * h, 0.0439f * w, 0.6051f * h)
                    cubicTo(0.0367f * w, 0.5740f * h, 0.0599f * w, 0.5319f * h, 0.0734f * w, 0.5000f * h)
                    cubicTo(0.0869f * w, 0.4681f * h, 0.1177f * w, 0.4447f * h, 0.1245f * w, 0.4136f * h)
                    cubicTo(0.1316f * w, 0.3826f * h, 0.1143f * w, 0.3481f * h, 0.1156f * w, 0.3136f * h)
                    cubicTo(0.1173f * w, 0.2787f * h, 0.1143f * w, 0.2306f * h, 0.1342f * w, 0.2060f * h)
                    cubicTo(0.1540f * w, 0.1809f * h, 0.2008f * w, 0.1732f * h, 0.2342f * w, 0.1638f * h)
                    cubicTo(0.2671f * w, 0.1545f * h, 0.3046f * w, 0.1638f * h, 0.3329f * w, 0.1502f * h)
                    cubicTo(0.3616f * w, 0.1362f * h, 0.3772f * w, 0.1009f * h, 0.4051f * w, 0.0809f * h)
                    cubicTo(0.4329f * w, 0.0604f * h, 0.4684f * w, 0.0281f * h, 0.5000f * w, 0.0281f * h)
                    cubicTo(0.5316f * w, 0.0281f * h, 0.5671f * w, 0.0604f * h, 0.5949f * w, 0.0809f * h)
                    cubicTo(0.6228f * w, 0.1009f * h, 0.6384f * w, 0.1362f * h, 0.6671f * w, 0.1502f * h)
                    cubicTo(0.6954f * w, 0.1638f * h, 0.7329f * w, 0.1545f * h, 0.7658f * w, 0.1638f * h)
                    cubicTo(0.7992f * w, 0.1732f * h, 0.8460f * w, 0.1809f * h, 0.8658f * w, 0.2060f * h)
                    cubicTo(0.8857f * w, 0.2306f * h, 0.8827f * w, 0.2787f * h, 0.8844f * w, 0.3136f * h)
                    cubicTo(0.8857f * w, 0.3481f * h, 0.8684f * w, 0.3826f * h, 0.8755f * w, 0.4136f * h)
                    cubicTo(0.8823f * w, 0.4447f * h, 0.9131f * w, 0.4681f * h, 0.9266f * w, 0.5000f * h)
                    close()
                }
            }
        }

        val outline = Outline.Generic(path)
        cachedWidth = size.width
        cachedHeight = size.height
        cachedOutline = outline
        return outline
    }
}
