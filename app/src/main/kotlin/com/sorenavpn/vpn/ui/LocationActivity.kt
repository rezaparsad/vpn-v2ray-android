package com.sorenavpn.vpn.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tencent.mmkv.MMKV
import com.sorenavpn.vpn.R
import com.sorenavpn.vpn.util.MmkvManager
import com.sorenavpn.vpn.viewmodel.MainViewModel


class LocationActivity : AppCompatActivity() {

    val mainViewModel: MainViewModel by viewModels()
    private var serverList = MmkvManager.decodeServerList()
    private val locations = ArrayList<ModelLocation>()
    private var adapter: RecyclerView.Adapter<RecyclerLocations.ViewHolder>? = null
    private val mainStorage by lazy { MMKV.mmkvWithID(MmkvManager.ID_MAIN, MMKV.MULTI_PROCESS_MODE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Location"
        setContentView(R.layout.activity_locations)
        mainViewModel.reloadServerList()

        val selected = mainStorage?.decodeString(MmkvManager.KEY_SELECTED_SERVER)

        for (guid in serverList) {
            val config = MmkvManager.decodeServerConfig(guid) ?: continue
            var model = ModelLocation()
            model.country = config.remarks
            model.speed = "265 MS"
            model.guid = guid
            locations.add(model)
            if (guid == selected) {
                model.selected = true
            }
        }
        var recyclerLocations: RecyclerView = findViewById(R.id.recycler_locations)
        recyclerLocations.setHasFixedSize(true)
        recyclerLocations.layoutManager = LinearLayoutManager(this)
        adapter = RecyclerLocations(this, locations)
        recyclerLocations.adapter = adapter
    }



}