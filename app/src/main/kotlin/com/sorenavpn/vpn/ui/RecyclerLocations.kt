package com.sorenavpn.vpn.ui

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tencent.mmkv.MMKV
import com.sorenavpn.vpn.R
import com.sorenavpn.vpn.util.MmkvManager

class RecyclerLocations(private val activity: LocationActivity, private val mList: List<ModelLocation>): RecyclerView.Adapter<RecyclerLocations.ViewHolder>() {

    private var mActivity: LocationActivity = activity
    private val mainStorage by lazy { MMKV.mmkvWithID(MmkvManager.ID_MAIN, MMKV.MULTI_PROCESS_MODE) }

    var isRunning = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.location, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("ResourceAsColor")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val guid = mActivity.mainViewModel.serversCache[position].guid
        if (mList[position].selected) {
            holder.layoutLocation.removeAllViews()
            val adView = activity.layoutInflater.inflate(R.layout.location_selected, null)
            adView.post(Runnable {
                val layoutParams = adView.layoutParams
                layoutParams.width = LinearLayout.LayoutParams.MATCH_PARENT
                adView.layoutParams = layoutParams
            })
            holder.layoutLocation.addView(adView)
            holder.imageLocation = holder.itemView.findViewById(R.id.image_location)
            holder.textCountry = holder.itemView.findViewById(R.id.country_location)
            holder.speedLocation = holder.itemView.findViewById(R.id.speed_location)
        }
        holder.itemView.setOnClickListener {
            val selected = mainStorage?.decodeString(MmkvManager.KEY_SELECTED_SERVER)
            if (guid != selected) {
                mainStorage?.encode(MmkvManager.KEY_SELECTED_SERVER, guid)
            }
            MainActivity.isSelectLocation = true
            activity.onBackPressed()
        }
        val itemsViewModel = mList[position]
        holder.imageLocation.setImageResource(itemsViewModel.getImage())
        holder.textCountry.text = itemsViewModel.country
        holder.speedLocation.text = itemsViewModel.speed

    }

    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        var imageLocation: ImageView = itemView.findViewById(R.id.image_location)
        var textCountry: TextView = itemView.findViewById(R.id.country_location)
        var speedLocation: TextView = itemView.findViewById(R.id.speed_location)
        val layoutLocation: LinearLayout = itemView.findViewById(R.id.layout_location)
    }

    override fun getItemCount(): Int {
        return mList.size
    }
}