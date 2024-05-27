package com.cavss.artravel.ui.view.screen.travel.theme

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.cavss.artravel.databinding.FragmentThemeBinding
import com.cavss.artravel.vm.AuthVM

class ThemeFragment : Fragment() {

    private lateinit var binding : FragmentThemeBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentThemeBinding.inflate(inflater,container,false)
        binding.run {
            lifecycleOwner = viewLifecycleOwner
        }

        return binding.root
    }



    private val authVM : AuthVM by activityViewModels()

    private fun createCard(){
        try{
            if (authVM.getAuth() != null){

            }else{

            }
        }catch (e:Exception){
            Log.e("mException", "CardFragment, createCard // Exception : ${e.localizedMessage}")
        }
    }
    private fun readCards(){
        try{

        }catch (e:Exception){
            Log.e("mException", "CardFragment, readCards // Exception : ${e.localizedMessage}")
        }
    }
    private fun deleteCard(){
        try{

        }catch (e:Exception){
            Log.e("mException", "CardFragment, deleteCard // Exception : ${e.localizedMessage}")
        }
    }
    private fun updateCard(){
        try{

        }catch (e:Exception){
            Log.e("mException", "CardFragment, updateCard // Exception : ${e.localizedMessage}")
        }
    }
    private fun saveCard(){
        try{

        }catch (e:Exception){
            Log.e("mException", "CardFragment, saveCard // Exception : ${e.localizedMessage}")
        }
    }

    private fun filteringCard(){
        try{

        }catch (e:Exception){
            Log.e("mException", "CardFragment, filteringCard // Exception : ${e.localizedMessage}")
        }
    }
    private fun paginateCard(){
        try{

        }catch (e:Exception){
            Log.e("mException", "CardFragment, paginateCard // Exception : ${e.localizedMessage}")
        }
    }

    private fun cardClick(){
        try{

        }catch (e:Exception){
            Log.e("mException", "CardFragment, cardClick // Exception : ${e.localizedMessage}")
        }
    }
}