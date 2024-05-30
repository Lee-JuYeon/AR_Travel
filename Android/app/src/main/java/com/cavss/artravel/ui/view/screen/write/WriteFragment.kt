package com.cavss.artravel.ui.view.screen.write

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cavss.artravel.BR
import com.cavss.artravel.R
import com.cavss.artravel.databinding.FragmentWriteBinding
import com.cavss.artravel.databinding.HeaderThemeWriteBinding
import com.cavss.artravel.databinding.HolderThemeWriteImageBinding
import com.cavss.artravel.models.CardModel
import com.cavss.artravel.models.ImageModel
import com.cavss.artravel.models.ThemeModel
import com.cavss.artravel.ui.custom.recyclerview.BaseAdapters
import com.cavss.artravel.ui.custom.recyclerview.CustomItemGap
import com.cavss.artravel.ui.custom.recyclerview.IClickListener
import com.cavss.artravel.ui.custom.recyclerview.StickyHeaderItemDecoration
import com.cavss.artravel.ui.view.screen.write.historylist.HistoryAdapter
import com.cavss.artravel.ui.view.screen.write.writelist.WriteListAdapter

class WriteFragment : Fragment() {
    private lateinit var binding : FragmentWriteBinding
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentWriteBinding.inflate(inflater, container, false)
        binding.run {
            setThemeHistoryList(themeHistoryList)
            setThemeWriteList(themeWriteList)
        }
        return binding.root
    }

    private var themeHistoryAdapter : HistoryAdapter? = null
    private fun setThemeHistoryList(recyclerView: RecyclerView){
        try{
            val itemClick = object : IClickListener<ThemeModel> {
                override fun onItemClick(model: ThemeModel, position: Int) {
                    when(model.themeUID){
                        "MAKE_NEW_THEME" -> {

                        }
                        else -> {

                        }
                    }
                }
            }

            if (themeHistoryAdapter == null){
                themeHistoryAdapter = HistoryAdapter()
                themeHistoryAdapter.let {
                    it?.updateList(listOf())
                    it?.setOnClick(itemClick)
                }
            }

            recyclerView.apply {
                adapter = themeHistoryAdapter
                setHasFixedSize(true)
                layoutManager = LinearLayoutManager(requireActivity()).apply{
                    orientation = LinearLayoutManager.HORIZONTAL
                    isItemPrefetchEnabled = false
                }
                addItemDecoration(CustomItemGap(10))
                setItemViewCacheSize(0)
            }

        }catch (e:Exception){
            Log.e("mException", "WriteFragment, setThemeHistoryList // Exception : ${e.localizedMessage}")
        }
    }

    private var cardsAdapter : WriteListAdapter? = null
    private fun setThemeWriteList(recyclerView: RecyclerView){
        try{
            val itemClick = object : IClickListener<CardModel> {
                override fun onItemClick(model: CardModel, position: Int) {

                }
            }

            if (cardsAdapter == null) {
                cardsAdapter = WriteListAdapter().apply {
                    updateList(listOf())
                    setOnClick(itemClick)
                }
            }

            recyclerView.apply {
                adapter = cardsAdapter
                setHasFixedSize(true)
                layoutManager = LinearLayoutManager(requireActivity()).apply{
                    orientation = LinearLayoutManager.VERTICAL
                    isItemPrefetchEnabled = false
                }
//                addItemDecoration(StickyHeaderItemDecoration { position ->
//                    position == 0 // 0번 위치에 있는 아이템이 헤더
//                })
                setItemViewCacheSize(0)
            }

            recyclerView.post {
                setHeaderUI()
            }
        }catch (e:Exception){
            Log.e("mException", "WriteFragment, setThemeWriteList // Exception : ${e.localizedMessage}")
        }
    }

    private var customAdapter : BaseAdapters.RecyclerAdapter<ImageModel, HolderThemeWriteImageBinding>? = null
    private fun setHeaderUI(){
        try{
            val header = cardsAdapter?.getHeaderHolder()
            header?.let {
                val clickEvent = object : IClickListener<ImageModel> {
                    override fun onItemClick(model: ImageModel, position: Int) {
                        when(model.imageUID){
                            "ADD_PHOTO" -> {
                                Log.e("mException", "이미지 추가해야함.")
                            }
                            else -> {

                            }
                        }
                    }
                }

                // setting adapter of recyclerview
                customAdapter = object : BaseAdapters.RecyclerAdapter<ImageModel, HolderThemeWriteImageBinding>(){
                    override fun setViewHolderXmlFileName(viewType: Int): Int {
                        return R.layout.holder_theme_write_image
                    }

                    override fun setViewHolderVariable(
                        position: Int,
                        model: ImageModel?
                    ): List<Pair<Int, Any>> {
                        return listOf(
                            BR.model to model!!, //
                            BR.position to position, // recyclerview viewholder position
                            BR.clickCallback to clickEvent // fetching recyclerview holder click event
                        )
                    }
                }.apply {
                    updateList(listOf(ImageModel("", "ADD_PHOTO", null)))
                }


                // setting recyclerview
                it.imageList.apply {
                    adapter = customAdapter
                    setHasFixedSize(true)
                    layoutManager = LinearLayoutManager(requireActivity()).apply{
                        orientation = LinearLayoutManager.HORIZONTAL
                        isItemPrefetchEnabled = false
                    }
                    addItemDecoration(CustomItemGap(10))
                    setItemViewCacheSize(0)
                }
            }
        }catch (e:Exception){
            Log.e("mException", "WriteFragment, setHeaderUI // Exception : ${e.localizedMessage}")
        }
    }
}