package com.music42.swiftyprotein.data.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Streaming

interface RcsbApi {

    @Streaming
    @GET("ligands/download/{id}.cif")
    suspend fun getLigandCif(@Path("id") ligandId: String): Response<ResponseBody>

    companion object {
        const val BASE_URL = "https://files.rcsb.org/"
    }
}
