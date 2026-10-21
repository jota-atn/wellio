package com.projeto.wellio.data.prefs

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream

/**
 * Foto recortada do perfil ou da capa (SPEC §9.1), num arquivo do armazenamento interno do
 * app; [prefix] separa as duas. Cada foto nova ganha um nome novo, para quem mostra a foto
 * saber que ela mudou.
 */
class ProfilePhotoStore(
    private val dir: File,
    private val prefix: String = "profile",
    private val now: () -> Long = System::currentTimeMillis
) {

    fun file(name: String): File = File(dir, name)

    /** Grava a foto com [write] e devolve o nome do arquivo; as fotos antigas são apagadas. */
    suspend fun save(write: (OutputStream) -> Unit): String = withContext(Dispatchers.IO) {
        dir.mkdirs()
        val name = "${prefix}_${now()}.jpg"
        val temp = File(dir, "$name.tmp")
        temp.outputStream().use(write)
        check(temp.renameTo(File(dir, name))) { "Não deu para salvar a foto" }
        removeAll(except = name)
        name
    }

    suspend fun clear() = withContext(Dispatchers.IO) { removeAll(except = null) }

    private fun removeAll(except: String?) {
        dir.listFiles { f -> f.name.startsWith("${prefix}_") && f.name != except }?.forEach { it.delete() }
    }
}
