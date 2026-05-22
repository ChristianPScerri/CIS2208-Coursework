package com.christian.quickcart

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.christian.quickcart.databinding.FragmentFirstBinding

/**
 * Home dashboard fragment that gives users quick access to core QuickCart areas.
 */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    /**
     * Creates the home dashboard view using the generated view binding class.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root

    }

    /**
     * Connects dashboard buttons to navigation or temporary placeholder feedback.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonOpenShoppingList.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }

        binding.buttonOpenPantry.setOnClickListener {
            com.google.android.material.snackbar.Snackbar.make(
                view,
                R.string.feature_coming_next,
                com.google.android.material.snackbar.Snackbar.LENGTH_LONG
            ).show()
        }

        binding.buttonOpenRecipes.setOnClickListener {
            com.google.android.material.snackbar.Snackbar.make(
                view,
                R.string.feature_coming_next,
                com.google.android.material.snackbar.Snackbar.LENGTH_LONG
            ).show()
        }
    }

    /**
     * Clears the view binding reference when the fragment view is destroyed.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
