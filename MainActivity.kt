package com.threestudiopro

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.MotionEvent
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.opengl.GLSurfaceView

class MainActivity : Activity() {
    private lateinit var view3d: ModelView
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(15, 18, 25))
        }
        root.addView(TextView(this).apply {
            text = "3D STUDIO PRO   •   WORKSPACE"
            setTextColor(Color.WHITE); textSize = 16f
            gravity = Gravity.CENTER_VERTICAL; setPadding(18, 12, 12, 12)
            setBackgroundColor(Color.rgb(24, 29, 40))
        }, LinearLayout.LayoutParams(-1, 56))
        status = TextView(this).apply {
            text = "Cube selected  •  Drag to orbit  •  Pinch to zoom"
            setTextColor(Color.LTGRAY); textSize = 12f; setPadding(12, 9, 12, 9)
        }
        root.addView(status, LinearLayout.LayoutParams(-1, 36))
        view3d = ModelView(this)
        root.addView(view3d, LinearLayout.LayoutParams(-1, 0, 1f))
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(5, 5, 5, 8)
            setBackgroundColor(Color.rgb(24, 29, 40))
        }
        val r1 = row()
        r1.addView(button("+ Cube") { view3d.addShape(ShapeType.CUBE); status.text = "Cube added" })
        r1.addView(button("+ Sphere") { view3d.addShape(ShapeType.SPHERE); status.text = "Sphere added" })
        r1.addView(button("+ Cylinder") { view3d.addShape(ShapeType.CYLINDER); status.text = "Cylinder added" })
        val r2 = row()
        r2.addView(button("Object") { status.text = "Object mode" })
        r2.addView(button("Edit") { status.text = "Edit mode preview — mesh editing is a later milestone" })
        r2.addView(button("Reset view") { view3d.resetView(); status.text = "View reset" })
        panel.addView(r1); panel.addView(r2)
        root.addView(panel, LinearLayout.LayoutParams(-1, 108))
        setContentView(root)
    }

    private fun row() = LinearLayout(this).apply { gravity = Gravity.CENTER }
    private fun button(label: String, action: () -> Unit) = Button(this).apply {
        text = label; textSize = 11f; isAllCaps = false; setTextColor(Color.WHITE)
        setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(48, 62, 83)))
        setOnClickListener { action() }
        layoutParams = LinearLayout.LayoutParams(0, 46, 1f).apply { setMargins(3, 2, 3, 2) }
    }

    override fun onPause() { super.onPause(); if (::view3d.isInitialized) view3d.onPause() }
    override fun onResume() { super.onResume(); if (::view3d.isInitialized) view3d.onResume() }
}

enum class ShapeType { CUBE, SPHERE, CYLINDER }

class ModelView(activity: Activity) : GLSurfaceView(activity) {
    private val scene = SceneRenderer()
    private var px = 0f
    private var py = 0f
    private var oldDistance = 0f
    init { setEGLContextClientVersion(2); setRenderer(scene); renderMode = RENDERMODE_CONTINUOUSLY }
    fun addShape(type: ShapeType) = scene.addShape(type)
    fun resetView() = scene.resetView()

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.pointerCount == 1) {
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> { px = e.x; py = e.y; return true }
                MotionEvent.ACTION_MOVE -> {
                    scene.orbit((e.x-px)*0.45f, (e.y-py)*0.45f); px=e.x; py=e.y; return true
                }
            }
        } else if (e.pointerCount >= 2) {
            val dx=e.getX(0)-e.getX(1); val dy=e.getY(0)-e.getY(1)
            val d=kotlin.math.sqrt(dx*dx+dy*dy)
            if (e.actionMasked == MotionEvent.ACTION_POINTER_DOWN) { oldDistance=d; return true }
            if (e.actionMasked == MotionEvent.ACTION_MOVE && oldDistance>0f) {
                scene.zoom((oldDistance-d)*0.008f); oldDistance=d; return true
            }
        }
        if (e.actionMasked == MotionEvent.ACTION_UP || e.actionMasked == MotionEvent.ACTION_CANCEL) oldDistance=0f
        return true
    }
}
