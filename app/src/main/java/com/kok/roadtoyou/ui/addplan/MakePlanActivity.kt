package com.kok.roadtoyou.ui.addplan

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.database.*
import com.kok.roadtoyou.DataConverter
import com.kok.roadtoyou.R
import com.kok.roadtoyou.ui.search.PlaceItem
import com.kok.roadtoyou.ui.search.SearchActivity
import kotlinx.android.synthetic.main.activity_make_plan.*

class MakePlanActivity : AppCompatActivity() {

    private val SELECT_BTN = 3000

    lateinit var mMap: GoogleMap

    lateinit var adapter: MakePlanViewPagerAdapter
    lateinit var planItem: PlanItem
    private var itemList = ArrayList<ArrayList<AddPlaceItem>>()

    lateinit var plansDB: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_make_plan)
        init()
    }

    private fun init() {
        initMap()
        initBtn()

        if (intent.hasExtra("ACTIVITY_FLAG")) {
            //from AddPlanFragment
            planItem = intent.getParcelableExtra("PLAN_ITEM")!!
            initView()
        } else {
            //from MyPageFragment
            initPlan()
        }
    }

    private fun initPlan() {
        val planID = intent.getStringExtra("PLAN_ID")
        plansDB = FirebaseDatabase.getInstance().getReference("plans")
        plansDB.orderByKey().equalTo(planID).addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
//                TODO("Not yet implemented")
            }

            override fun onDataChange(p0: DataSnapshot) {
                planItem = DataConverter().dataConvertPlanItem(p0.value.toString())
                Log.d("Log_Plan_Item",planItem.toString())
                initView()
            }
        })
    }

    private fun initBtn() {
        //장소 추가 버튼
        addPlaceBtn.setOnClickListener {
            if (itemList[viewpager_make_plan.currentItem].size >= 50) {
                Toast.makeText(this, "각 날짜의 일정은 50개를 넘길 수 없습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent(this, SearchActivity::class.java)
            intent.putExtra("FLAG", true)
            startActivityForResult(intent, SELECT_BTN)
        }

        //메모 추가 버튼
        addMemoBtn.setOnClickListener {
            //TODO: memo
        }
    }

    private fun initView() {
        tv_plan_date.text = planItem.period

        itemList.clear()
        for (i in 0 until planItem.days!!) {
            val temp = ArrayList<AddPlaceItem>()
            itemList.add(temp)
        }
        val placeList = planItem.placeList?.sortedWith(compareBy{ it.count })
        if (placeList != null) {
            for (i in placeList.indices) {
                itemList[placeList[i].date!!].add(placeList[i])
            }
        }

        adapter = MakePlanViewPagerAdapter(itemList)
        viewpager_make_plan.adapter = adapter

        TabLayoutMediator(tabLayout_make_plan, viewpager_make_plan) { tab, position ->
            tab.text =  "DAY ${position+1}"
        }.attach()

        //TabLayout Mode 변경
        if (tabLayout_make_plan.tabCount > 6)
            tabLayout_make_plan.tabMode = TabLayout.MODE_SCROLLABLE
    }

    private fun initMap() {
        val defaultLoc = LatLng(36.38, 127.51)     //남한 중심 좌표 - 괴산군
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync{
            mMap = it
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLoc, 9f))
            mMap.setMinZoomPreference(6.0f)       //최소 줌
            mMap.setMaxZoomPreference(18.0f)      //최대 줌
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                //SearchActivity 에서 "선택"버튼으로 넘어왔을 때
                SELECT_BTN -> uploadData(data)
            }
        }
    }

    //Firebase PlaceList 에 추가
    private fun uploadData(data: Intent?) {
        if (data != null) {
            val placeItem = data.getParcelableExtra<PlaceItem>("PLACE_DATA") ?: return
            val selectDate = viewpager_make_plan.currentItem
            val tempItem = AddPlaceItem(
                selectDate,
                itemList[selectDate].size +1,
                placeItem.title,
                placeItem.id,
                placeItem.type
            )
            plansDB = FirebaseDatabase.getInstance().getReference("plans/${planItem.planID}")
            plansDB.child("placeList/${placeItem.id}").setValue(tempItem)
            itemList[selectDate].add(tempItem)
            adapter.notifyDataSetChanged()
        } else {
            Log.d("Error", "Data Intent is null")
        }
    }
}

