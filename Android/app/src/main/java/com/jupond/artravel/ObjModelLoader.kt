package com.jupond.artravel

import android.content.Context
import android.util.Log
import de.javagl.obj.Obj
import de.javagl.obj.ObjData
import de.javagl.obj.ObjReader
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer

class ObjModelLoader(private val context: Context) {
    private val TAG = "ObjModelLoader"

    // 모델 데이터를 저장할 구조체
    data class ModelData(
        val vertexBuffer: FloatBuffer,
        val normalBuffer: FloatBuffer?,
        val texCoordBuffer: FloatBuffer?,
        val indexBuffer: IntBuffer,
        val numIndices: Int
    )

    fun loadModelFromAssets(modelPath: String): ModelData? {
        try {
            // 에셋 폴더에서 모델 파일 로드
            context.assets.open(modelPath).use { inputStream ->
                // OBJ 파일 파싱
                val obj = ObjReader.read(inputStream)

                // 버텍스 데이터 추출
                val vertices = ObjData.getVerticesArray(obj)
                val vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer()
                vertexBuffer.put(vertices)
                vertexBuffer.position(0)

                // 법선 데이터 추출 (없을 수도 있음)
                val normalBuffer: FloatBuffer? = if (obj.numNormals > 0) {
                    val normals = ObjData.getNormalsArray(obj)
                    ByteBuffer.allocateDirect(normals.size * 4)
                        .order(ByteOrder.nativeOrder())
                        .asFloatBuffer()
                        .apply {
                            put(normals)
                            position(0)
                        }
                } else null

                // 텍스처 좌표 데이터 추출 (없을 수도 있음)
                val texCoordBuffer: FloatBuffer? = if (obj.numTexCoords > 0) {
                    val texCoords = ObjData.getTexCoordsArray(obj, 2)
                    ByteBuffer.allocateDirect(texCoords.size * 4)
                        .order(ByteOrder.nativeOrder())
                        .asFloatBuffer()
                        .apply {
                            put(texCoords)
                            position(0)
                        }
                } else null

                // 인덱스 데이터 추출
                val indices = ObjData.getFaceVertexIndicesArray(obj)
                val indexBuffer = ByteBuffer.allocateDirect(indices.size * 4)
                    .order(ByteOrder.nativeOrder())
                    .asIntBuffer()
                indexBuffer.put(indices)
                indexBuffer.position(0)

                Log.d(TAG, "모델 로드 완료: $modelPath, 버텍스: ${obj.numVertices}, 페이스: ${indices.size/3}")

                return ModelData(
                    vertexBuffer = vertexBuffer,
                    normalBuffer = normalBuffer,
                    texCoordBuffer = texCoordBuffer,
                    indexBuffer = indexBuffer,
                    numIndices = indices.size
                )
            }
        } catch (e: IOException) {
            Log.e(TAG, "모델 로드 실패: $modelPath", e)
            return null
        } catch (e: Exception) {
            Log.e(TAG, "모델 처리 중 오류: ${e.message}", e)
            return null
        }
    }
}