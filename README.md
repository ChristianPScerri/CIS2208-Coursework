# QuickCart

QuickCart is an Android mobile application developed for the CIS2208 Mobile Computing coursework. The app is designed to help users manage shopping lists, track pantry items, attach product or receipt images, share shopping lists, and view recipe suggestions based on available pantry ingredients.

## Project Overview

Many users forget grocery items, buy duplicate products, or lose track of what they already have at home. QuickCart aims to solve this by combining a shopping list with pantry management in one simple mobile application.

The app is intended for use at home and while shopping. Users can prepare their shopping list, mark items as bought, move products into the pantry, and use pantry ingredients to find recipe ideas.

## Main Features

- Add, edit, delete, and search shopping list items
- Store item details such as name, quantity, amount, category, priority, and notes
- Mark shopping items as bought
- Manage pantry items separately from shopping list items
- Add expiry dates to pantry products
- Upload or capture product/receipt images
- Share the shopping list using Android share intent
- Get recipe suggestions using pantry ingredients
- Store shopping and pantry data locally using SQLite

## Technologies Used

- Kotlin
- Android Studio
- Android Activities
- Fragments
- Navigation Component
- XML Layouts
- RecyclerView
- SQLite local database
- Android Intents
- Image upload / camera intent
- Share intent
- HTTP requests
- Material UI components

## App Screens

The application includes the following main screens:

- Home screen
- Shopping list screen
- Add/edit shopping item screen
- Pantry screen
- Add/edit pantry item screen
- Recipe suggestions screen

## Local Storage

QuickCart uses SQLite to store shopping list and pantry data locally on the device. This allows the user to continue using the main features of the app even when internet access is unavailable.

## Recipe Suggestions

The recipe feature uses HTTP requests to retrieve recipe suggestions based on selected pantry ingredients. Results are displayed in a list and can be opened by the user for more information.

## Image Upload

Users can attach an image to an item by either taking a photo or selecting an image from the gallery. This can be used for product images, labels, or receipts.

## Sharing

The shopping list can be shared through other Android apps such as messaging, email, or notes using Android’s share intent.

## How to Run the Project

1. Clone the repository:

```bash
git clone https://github.com/ChristianPScerri/CIS2208-Coursework.git
