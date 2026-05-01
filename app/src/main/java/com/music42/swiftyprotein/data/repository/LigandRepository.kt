package com.music42.swiftyprotein.data.repository

import android.content.Context
import com.music42.swiftyprotein.R
import com.music42.swiftyprotein.data.model.Ligand
import com.music42.swiftyprotein.data.parser.CifParser
import com.music42.swiftyprotein.data.remote.RcsbApi
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LigandRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val rcsbApi: RcsbApi
) {

    private var ligandIds: List<String>? = null

    private fun cifCacheFile(ligandId: String): File {
        val safe = ligandId.trim().uppercase()
            .replace(Regex("[^A-Z0-9_-]"), "_")
        return File(context.filesDir, "cif_cache/$safe.cif")
    }

    suspend fun getLigandIds(): List<String> = withContext(Dispatchers.IO) {
        ligandIds ?: run {
            val ids = context.resources.openRawResource(R.raw.ligands)
                .bufferedReader()
                .readLines()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            ligandIds = ids
            ids
        }
    }

    data class LigandCacheInfo(val formula: String, val atomCount: Int)

    suspend fun getCachedInfo(ligandId: String): LigandCacheInfo? = withContext(Dispatchers.IO) {
        val file = cifCacheFile(ligandId)
        if (!file.exists()) return@withContext null
        try {
            val ligand = CifParser.parse(ligandId, file.readText())
            if (ligand.atoms.isEmpty()) null
            else LigandCacheInfo(ligand.formula, ligand.atoms.size)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun fetchLigand(ligandId: String): Result<Ligand> = withContext(Dispatchers.IO) {
        return@withContext fetchLigand(ligandId, null)
    }

    suspend fun fetchLigand(
        ligandId: String,
        onProgress: ((stage: String, progress: Float) -> Unit)?
    ): Result<Ligand> = withContext(Dispatchers.IO) {
        try {
            val cacheFile = cifCacheFile(ligandId)
            if (cacheFile.exists()) {
                onProgress?.invoke("Reading cache", 0.10f)
                val cached = cacheFile.readText()
                onProgress?.invoke("Parsing ligand", 0.35f)
                val ligand = CifParser.parse(ligandId, cached)
                if (ligand.atoms.isNotEmpty()) {
                    onProgress?.invoke("Preparing scene", 0.90f)
                    return@withContext Result.success(ligand)
                }
                
            }

            onProgress?.invoke("Downloading CIF", 0.20f)
            val response = rcsbApi.getLigandCif(ligandId)
            if (!response.isSuccessful) {
                if (response.code() == 404) {
                    return@withContext Result.failure(
                        Exception("Ligand not found (404). This ligand may not exist in the database.")
                    )
                }
                return@withContext Result.failure(
                    Exception("Server returned ${response.code()}: ${response.message()}")
                )
            }
            val body = response.body()?.string()
                ?: return@withContext Result.failure(Exception("Empty response body"))

            onProgress?.invoke("Saving cache", 0.30f)
            cacheFile.parentFile?.mkdirs()
            runCatching { cacheFile.writeText(body) }

            onProgress?.invoke("Parsing ligand", 0.55f)
            val ligand = CifParser.parse(ligandId, body)
            if (ligand.atoms.isEmpty()) {
                return@withContext Result.failure(
                    Exception("Failed to parse ligand data. The file may be corrupted.")
                )
            }
            onProgress?.invoke("Preparing scene", 0.90f)
            Result.success(ligand)
        } catch (e: UnknownHostException) {
            Result.failure(Exception("No internet connection. Please check your network."))
        } catch (e: SocketTimeoutException) {
            Result.failure(Exception("Request timeout. Please try again."))
        } catch (e: java.io.IOException) {
            Result.failure(Exception("No internet connection. Please check your network."))
        } catch (e: Exception) {
            
            val msg = e.localizedMessage?.takeIf { it.isNotBlank() } ?: "Unknown error"
            Result.failure(Exception(msg))
        }
    }
}
