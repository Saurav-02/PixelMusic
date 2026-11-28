package com.saurav.pixelmusic.presentation.components.lyrics.pixelmusic

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import androidx.annotation.RequiresApi

/*
 * AGSL shader plumbing, kept behind `Any`.
 *
 * `android.graphics.RuntimeShader` is API 33. Naming it in a parameter, return type or field
 * means ART has to resolve the class when that method is reached - and on Android 8 that is a
 * `NoClassDefFoundError: Failed resolution of: Landroid/graphics/RuntimeShader;` at launch, even
 * though every *call* is already behind an SDK check. Guarding the call is not enough; the type
 * must not appear in any signature reachable on an older device.
 *
 * So shaders travel as `Any` and only these helpers - each annotated, each invoked solely from
 * inside a version check - ever name the real type.
 */

@RequiresApi(33)
internal fun createAgslShader(source: String): Any = RuntimeShader(source)

@RequiresApi(33)
internal fun Any.setAgslUniform(name: String, value: Float) {
    (this as RuntimeShader).setFloatUniform(name, value)
}

@RequiresApi(33)
internal fun Any.setAgslUniform(name: String, first: Float, second: Float) {
    (this as RuntimeShader).setFloatUniform(name, first, second)
}

@RequiresApi(33)
internal fun Any.setAgslUniform(name: String, first: Float, second: Float, third: Float) {
    (this as RuntimeShader).setFloatUniform(name, first, second, third)
}

@RequiresApi(33)
internal fun Any.setAgslUniform(name: String, first: Float, second: Float, third: Float, fourth: Float) {
    (this as RuntimeShader).setFloatUniform(name, first, second, third, fourth)
}

// a uniform array: a float4 list of 16 takes 64 numbers, in order
@RequiresApi(33)
internal fun Any.setAgslUniformArray(name: String, values: FloatArray) {
    (this as RuntimeShader).setFloatUniform(name, values)
}

/** For a `layout(color) uniform half4`: [color] is an sRGB ARGB int, converted for the shader. */
@RequiresApi(33)
internal fun Any.setAgslColorUniform(name: String, color: Int) {
    (this as RuntimeShader).setColorUniform(name, color)
}

@RequiresApi(33)
internal fun agslRenderEffect(shader: Any, inputName: String): RenderEffect =
    RenderEffect.createRuntimeShaderEffect(shader as RuntimeShader, inputName)

@RequiresApi(33)
internal fun Any.setAgslInputShader(name: String, input: android.graphics.Shader) {
    (this as RuntimeShader).setInputShader(name, input)
}

/** The shader itself, to draw with (a brush/paint shader rather than a layer effect). */
@RequiresApi(33)
internal fun Any.asAgslPaintShader(): android.graphics.Shader = this as RuntimeShader
