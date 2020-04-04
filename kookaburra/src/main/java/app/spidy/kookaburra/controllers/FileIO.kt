package app.spidy.kookaburra.controllers

import android.content.Context
import android.os.Environment
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class FileIO(private val context: Context) {
    fun saveTextFile(fileName: String, content: String) {
        val fileDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        assert(fileDir != null)
        if (!fileDir!!.exists()) {
            fileDir.mkdirs()
        }

        val file = File(fileDir, fileName)
        try {
            if (!file.exists()) {
                if (!file.createNewFile()) {
                    throw IOException("Unable to create file")
                }
            }
            val fos = FileOutputStream(file)
            val data = content.toByteArray()
            fos.write(data)
            fos.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun saveBytes(folderName: String, fileName: String, content: ByteArray) {
        val fileDir = context.getExternalFilesDir(folderName)
        assert(fileDir != null)
        if (!fileDir!!.exists()) {
            fileDir.mkdirs()
        }

        val file = File(fileDir, fileName)
        try {
            if (!file.exists()) {
                if (!file.createNewFile()) {
                    throw IOException("Unable to create file")
                }
            }
            val fos = FileOutputStream(file)
            fos.write(content)
            fos.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun readBytes(folderName: String, fileName: String): ByteArray {
        val fileDir = context.getExternalFilesDir(folderName)
        val file = File(fileDir, fileName)
        val fos = FileInputStream(file)
        val data = fos.readBytes()
        fos.close()
        return data
    }

    fun deleteFile(folderName: String, fileName: String) {
        val fileDir = context.getExternalFilesDir(folderName)
        val file = File(fileDir, fileName)
        if (file.exists()) {
            file.delete()
        }
    }
}