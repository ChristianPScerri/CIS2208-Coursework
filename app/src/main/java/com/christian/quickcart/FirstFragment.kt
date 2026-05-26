package com.christian.quickcart

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.christian.quickcart.databinding.FragmentFirstBinding
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * Home dashboard fragment that gives users quick access to core QuickCart areas.
 */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null
    private lateinit var databaseHelper: ShoppingDatabaseHelper

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

        databaseHelper = ShoppingDatabaseHelper(requireContext())
        databaseHelper.seedSampleItemsIfEmpty()
        databaseHelper.seedSamplePantryItemsIfEmpty()
        updateDashboardSummary()

        binding.buttonOpenShoppingList.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }

        binding.buttonOpenPantry.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_PantryFragment)
        }

        binding.buttonOpenRecipes.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_RecipesFragment)
        }
    }

    /**
     * Refreshes the dashboard with live shopping and pantry counts from SQLite.
     */
    override fun onResume() {
        super.onResume()
        if (::databaseHelper.isInitialized) {
            updateDashboardSummary()
        }
    }

    /**
     * Builds a short overview that is useful during quick shopping sessions.
     */
    private fun updateDashboardSummary() {
        val shoppingItems = databaseHelper.getAllShoppingItems()
        val pantryItems = databaseHelper.getAllPantryItems()
        val neededUnits = shoppingItems.sumOf { it.amount }
        val pantryUnits = pantryItems.sumOf { it.amount }
        val urgentExpiry = findMostUrgentExpiry(pantryItems)

        binding.textviewStatusBody.text = """
            Needed shopping units: $neededUnits
            Pantry stock units: $pantryUnits (${pantryItems.size} entries)
            Urgent pantry item: $urgentExpiry
        """.trimIndent()
    }

    /**
     * Finds the recently expired item that needs attention first, or the closest upcoming expiry.
     */
    private fun findMostUrgentExpiry(items: List<PantryItem>): String {
        val today = LocalDate.now()
        val datedItems = items.mapNotNull { item ->
            parseExpiryDate(item.expiryDate)?.let { expiryDate ->
                item to expiryDate
            }
        }

        if (datedItems.isEmpty()) {
            return "No expiry dates set"
        }

        val mostUrgent = datedItems.sortedWith(
            compareBy<Pair<PantryItem, LocalDate>> { (_, expiryDate) ->
                when {
                    expiryDate.isBefore(today) -> 0
                    expiryDate.isEqual(today) -> 1
                    else -> 2
                }
            }.thenComparator { (_, firstDate), (_, secondDate) ->
                when {
                    firstDate.isBefore(today) && secondDate.isBefore(today) ->
                        secondDate.compareTo(firstDate)
                    else -> firstDate.compareTo(secondDate)
                }
            }
        ).first()

        val item = mostUrgent.first
        val expiryDate = mostUrgent.second
        val status = when {
            expiryDate.isBefore(today) -> "Expired"
            expiryDate.isEqual(today) -> "Expires today"
            else -> "Next expiry"
        }
        return "$status: ${item.name} (${item.amount} x ${item.quantity}) - $expiryDate"
    }

    /**
     * Parses pantry expiry dates saved by the date picker.
     */
    private fun parseExpiryDate(expiryDate: String): LocalDate? {
        return try {
            LocalDate.parse(expiryDate)
        } catch (exception: DateTimeParseException) {
            null
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
