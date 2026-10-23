package com.snappurge.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.*
import kotlin.random.Random

private enum class ParticleType {
    RIBBON,  // 彩带纸片
    DISC,    // 圆形碎屑
    STAR     // 闪烁星芒
}

private class ParticleInit(
    val x0: Float,
    val y0: Float,
    val vx0: Float,
    val vy0: Float,
    val color: Color,
    val size: Float,
    val aspectRatio: Float,
    val type: ParticleType,
    val rot0: Float,
    val rotSpeed: Float,
    val swayFreq: Float,
    val swayAmp: Float,
    val dragK: Float,
    val gravity: Float
)

/**
 * 清理完成时的撒花 & 烟花全屏庆祝粒子动效
 * 采用 Animatable 驱动 Draw 阶段每帧刷新，配合解析物理方程，杜绝被主线程 UI 阻塞卡顿
 */
@Composable
fun CelebrationEffect(
    trigger: Int,
    modifier: Modifier = Modifier
) {
    if (trigger <= 0) return

    val progress = remember(trigger) { Animatable(0f) }

    // 生成纯函数物理初态（不可变，无状态累积）
    val particles = remember(trigger) {
        val list = mutableListOf<ParticleInit>()
        val palette = listOf(
            Color(0xFFFF1744), // 珊瑚红
            Color(0xFFFF9100), // 活力橙
            Color(0xFFFFEA00), // 金黄
            Color(0xFF00E676), // 鲜绿
            Color(0xFF00E5FF), // 亮青
            Color(0xFF2979FF), // 湛蓝
            Color(0xFFD500F9), // 炫紫
            Color(0xFFFF4081)  // 樱粉
        )

        // 1. 左右双礼炮（共 90 颗向上喷射）
        for (i in 0 until 90) {
            val fromLeft = i % 2 == 0
            val x0 = if (fromLeft) 0.15f else 0.85f
            val baseAngle = if (fromLeft) 58f else 122f
            val angle = baseAngle + Random.nextFloat() * 34f - 17f
            val rad = Math.toRadians(angle.toDouble()).toFloat()
            val speed = Random.nextFloat() * 24f + 15f

            val type = when (Random.nextInt(10)) {
                in 0..5 -> ParticleType.RIBBON
                in 6..8 -> ParticleType.DISC
                else -> ParticleType.STAR
            }

            list.add(
                ParticleInit(
                    x0 = x0,
                    y0 = 0.92f,
                    vx0 = cos(rad) * speed,
                    vy0 = -sin(rad) * speed,
                    color = palette.random(),
                    size = Random.nextFloat() * 8f + 10f,
                    aspectRatio = Random.nextFloat() * 1.5f + 1.2f,
                    type = type,
                    rot0 = Random.nextFloat() * 360f,
                    rotSpeed = Random.nextFloat() * 450f - 225f,
                    swayFreq = Random.nextFloat() * 4f + 2f,
                    swayAmp = Random.nextFloat() * 0.04f + 0.015f,
                    dragK = Random.nextFloat() * 0.3f + 0.7f,
                    gravity = Random.nextFloat() * 4f + 16f
                )
            )
        }

        // 2. 中心璀璨烟花绽放（35 颗全向辐射）
        for (i in 0 until 35) {
            val rad = Random.nextFloat() * 2f * PI.toFloat()
            val speed = Random.nextFloat() * 12f + 4f

            list.add(
                ParticleInit(
                    x0 = 0.50f + Random.nextFloat() * 0.08f - 0.04f,
                    y0 = 0.32f + Random.nextFloat() * 0.08f - 0.04f,
                    vx0 = cos(rad) * speed,
                    vy0 = sin(rad) * speed,
                    color = palette.random(),
                    size = Random.nextFloat() * 7f + 7f,
                    aspectRatio = 1f,
                    type = if (Random.nextBoolean()) ParticleType.STAR else ParticleType.DISC,
                    rot0 = Random.nextFloat() * 360f,
                    rotSpeed = Random.nextFloat() * 360f - 180f,
                    swayFreq = Random.nextFloat() * 3f + 1.5f,
                    swayAmp = Random.nextFloat() * 0.03f + 0.01f,
                    dragK = 1.1f,
                    gravity = 14f
                )
            )
        }

        list
    }

    LaunchedEffect(trigger) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 3200, easing = LinearEasing)
        )
    }

    if (progress.value < 1f) {
        Canvas(modifier = modifier.fillMaxSize()) {
            val p = progress.value // 触发 Compose Draw 阶段每帧刷新
            val t = p * 3.2f       // 动画经过的绝对时间（秒）
            val w = size.width
            val h = size.height

            // 尾声淡出曲线
            val globalAlpha = if (p > 0.65f) {
                ((1f - p) / 0.35f).coerceIn(0f, 1f)
            } else 1f

            particles.forEach { pt ->
                // 空气阻力下的经典解析物理轨迹，零累积误差，每帧精准还原：
                val expTerm = 1f - exp(-pt.dragK * t)
                val dragX = (pt.vx0 / pt.dragK) * expTerm / 11.5f
                val dragY = (pt.vy0 / pt.dragK) * expTerm / 11.5f
                val gravY = (pt.gravity / (pt.dragK * pt.dragK)) * (pt.dragK * t - expTerm) / 11.5f
                val swayX = pt.swayAmp * sin(pt.swayFreq * t)

                val normX = pt.x0 + dragX + swayX
                val normY = pt.y0 + dragY + gravY

                val px = normX * w
                val py = normY * h

                if (px in -50f..w + 50f && py in -50f..h + 50f && globalAlpha > 0.01f) {
                    val colorWithAlpha = pt.color.copy(alpha = globalAlpha)
                    val rotation = pt.rot0 + pt.rotSpeed * t

                    rotate(rotation, pivot = Offset(px, py)) {
                        when (pt.type) {
                            ParticleType.RIBBON -> {
                                val flipScale = abs(cos(rotation * 0.04f))
                                val pw = pt.size * flipScale
                                val ph = pt.size * pt.aspectRatio
                                drawRect(
                                    color = colorWithAlpha,
                                    topLeft = Offset(px - pw / 2f, py - ph / 2f),
                                    size = Size(pw.coerceAtLeast(2f), ph)
                                )
                            }
                            ParticleType.DISC -> {
                                drawCircle(
                                    color = colorWithAlpha,
                                    radius = pt.size / 2f,
                                    center = Offset(px, py)
                                )
                            }
                            ParticleType.STAR -> {
                                val r = pt.size
                                val starPath = Path().apply {
                                    moveTo(px, py - r)
                                    quadraticTo(px, py, px + r, py)
                                    quadraticTo(px, py, px, py + r)
                                    quadraticTo(px, py, px - r, py)
                                    quadraticTo(px, py, px, py - r)
                                    close()
                                }
                                drawPath(path = starPath, color = colorWithAlpha)
                            }
                        }
                    }
                }
            }
        }
    }
}
