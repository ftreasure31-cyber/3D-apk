package com.threestudiopro

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.sin

private class Mesh(data: FloatArray) {
    val buffer: FloatBuffer = ByteBuffer.allocateDirect(data.size*4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply { put(data); position(0) }
    val count = data.size/3
    companion object {
        fun cube(): Mesh {
            val v=floatArrayOf(
                -1f,-1f,1f, 1f,-1f,1f, 1f,1f,1f, -1f,-1f,1f, 1f,1f,1f, -1f,1f,1f,
                -1f,-1f,-1f, -1f,1f,-1f, 1f,1f,-1f, -1f,-1f,-1f, 1f,1f,-1f, 1f,-1f,-1f,
                -1f,1f,-1f, -1f,1f,1f, 1f,1f,1f, -1f,1f,-1f, 1f,1f,1f, 1f,1f,-1f,
                -1f,-1f,-1f, 1f,-1f,-1f, 1f,-1f,1f, -1f,-1f,-1f, 1f,-1f,1f, -1f,-1f,1f,
                1f,-1f,-1f, 1f,1f,-1f, 1f,1f,1f, 1f,-1f,-1f, 1f,1f,1f, 1f,-1f,1f,
                -1f,-1f,-1f, -1f,-1f,1f, -1f,1f,1f, -1f,-1f,-1f, -1f,1f,1f, -1f,1f,-1f)
            return Mesh(v)
        }
        fun sphere(): Mesh {
            val a=ArrayList<Float>(); val rings=12; val seg=20
            fun point(u:Float,v:Float) { val th=u*Math.PI*2; val ph=v*Math.PI
                a.add((kotlin.math.sin(ph)*kotlin.math.cos(th)).toFloat()); a.add(kotlin.math.cos(ph).toFloat()); a.add((kotlin.math.sin(ph)*kotlin.math.sin(th)).toFloat()) }
            for(y in 0 until rings) for(x in 0 until seg) {
                val u=x.toFloat()/seg; val un=(x+1).toFloat()/seg; val v=y.toFloat()/rings; val vn=(y+1).toFloat()/rings
                for(q in arrayOf(floatArrayOf(u,v),floatArrayOf(un,v),floatArrayOf(un,vn),floatArrayOf(u,v),floatArrayOf(un,vn),floatArrayOf(u,vn))) point(q[0],q[1])
            }
            return Mesh(a.toFloatArray())
        }
        fun cylinder(): Mesh {
            val a=ArrayList<Float>(); val n=24
            for(i in 0 until n) { val t=i*Math.PI*2/n; val q=(i+1)*Math.PI*2/n
                val x=cos(t).toFloat(); val z=sin(t).toFloat(); val X=cos(q).toFloat(); val Z=sin(q).toFloat()
                val v=floatArrayOf(x,-1f,z,X,-1f,Z,X,1f,Z,x,-1f,z,X,1f,Z,x,1f,z,0f,1f,0f,x,1f,z,X,1f,Z,0f,-1f,0f,X,-1f,Z,x,-1f,z)
                v.forEach { a.add(it) }
            }
            return Mesh(a.toFloatArray())
        }
    }
}
private data class Item(val type:ShapeType,val x:Float,val mesh:Mesh)

class SceneRenderer : GLSurfaceView.Renderer {
    private var program=0; private var pos=0; private var col=0; private var mvpLoc=0
    private val projection=FloatArray(16); private val view=FloatArray(16); private val model=FloatArray(16); private val temp=FloatArray(16); private val mvp=FloatArray(16)
    @Volatile private var yaw=25f; @Volatile private var pitch=18f; @Volatile private var distance=6f
    private val items= mutableListOf<Item>(); private var index=0

    override fun onSurfaceCreated(gl:GL10?,config:EGLConfig?) {
        GLES20.glClearColor(0.075f,0.09f,0.12f,1f); GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        val vs="uniform mat4 uMVP; attribute vec4 aPosition; void main(){ gl_Position=uMVP*aPosition; }"
        val fs="precision mediump float; uniform vec4 uColor; void main(){ gl_FragColor=uColor; }"
        val v=shader(GLES20.GL_VERTEX_SHADER,vs); val f=shader(GLES20.GL_FRAGMENT_SHADER,fs)
        program=GLES20.glCreateProgram(); GLES20.glAttachShader(program,v); GLES20.glAttachShader(program,f); GLES20.glLinkProgram(program)
        pos=GLES20.glGetAttribLocation(program,"aPosition"); col=GLES20.glGetUniformLocation(program,"uColor"); mvpLoc=GLES20.glGetUniformLocation(program,"uMVP")
        synchronized(items) { if(items.isEmpty()) items.add(Item(ShapeType.CUBE,0f,Mesh.cube())) }
    }
    private fun shader(type:Int,source:String):Int { val s=GLES20.glCreateShader(type); GLES20.glShaderSource(s,source); GLES20.glCompileShader(s); return s }
    override fun onSurfaceChanged(gl:GL10?,w:Int,h:Int) { GLES20.glViewport(0,0,w,h); val r=w.toFloat()/h.coerceAtLeast(1); Matrix.frustumM(projection,0,-r,r,-1f,1f,2f,30f) }
    override fun onDrawFrame(gl:GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT); GLES20.glUseProgram(program)
        val y=Math.toRadians(yaw.toDouble()); val p=Math.toRadians(pitch.toDouble())
        val cx=(distance*cos(p)*sin(y)).toFloat(); val cy=(distance*sin(p)).toFloat(); val cz=(distance*cos(p)*cos(y)).toFloat()
        Matrix.setLookAtM(view,0,cx,cy,cz,0f,0f,0f,0f,1f,0f)
        synchronized(items) { items.forEach { item ->
            Matrix.setIdentityM(model,0); Matrix.translateM(model,0,item.x,0f,0f); Matrix.multiplyMM(temp,0,view,0,model,0); Matrix.multiplyMM(mvp,0,projection,0,temp,0)
            GLES20.glUniformMatrix4fv(mvpLoc,1,false,mvp,0); GLES20.glEnableVertexAttribArray(pos)
            item.mesh.buffer.position(0); GLES20.glVertexAttribPointer(pos,3,GLES20.GL_FLOAT,false,0,item.mesh.buffer)
            val c=when(item.type) { ShapeType.CUBE -> floatArrayOf(.22f,.63f,.92f,1f); ShapeType.SPHERE -> floatArrayOf(.95f,.48f,.20f,1f); ShapeType.CYLINDER -> floatArrayOf(.35f,.78f,.48f,1f) }
            GLES20.glUniform4fv(col,1,c,0); GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,item.mesh.count); GLES20.glDisableVertexAttribArray(pos)
        } }
    }
    fun addShape(t:ShapeType) { synchronized(items) { val x=((index%5)-2)*0.8f; index++; val m=when(t){ShapeType.CUBE->Mesh.cube();ShapeType.SPHERE->Mesh.sphere();ShapeType.CYLINDER->Mesh.cylinder()}; items.add(Item(t,x,m)) } }
    fun orbit(dx:Float,dy:Float) { yaw+=dx; pitch=(pitch+dy).coerceIn(-75f,75f) }
    fun zoom(d:Float) { distance=(distance+d).coerceIn(3f,14f) }
    fun resetView() { yaw=25f; pitch=18f; distance=6f }
}
