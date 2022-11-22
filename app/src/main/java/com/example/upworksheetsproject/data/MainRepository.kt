package com.example.upworksheetsproject.data

import android.util.Log
import javax.inject.Inject

class MainRepository @Inject constructor(){

    suspend fun getCompanyData(): Map<String, List<String>>? {
        return try {
            var data = SheetsApi.instance.getCompany()
            Log.e("TestPlay", data)
            val map = mutableMapOf<String, List<String>>()
            data = data.substring(1, data.length-2)
            Log.e("TestPlay", data)
            for(i in data.split("/")) {
                val company = i.split(",")[0]
                val activity = i.split(",")[1]
                if(map[company] != null) {
                    val updatedList = mutableListOf<String>()
                    val oldList = map[company]!!
                    updatedList.addAll(oldList)
                    updatedList.add(activity)
                    map[company] = updatedList
                } else{
                    val newList = mutableListOf<String>()
                    newList.add(activity)
                    map[company] = newList
                }
            }
            map
        }catch (e:Exception) {
            Log.e("TestPlay", "Repo: ${e.message}")
            null
        }
    }
}