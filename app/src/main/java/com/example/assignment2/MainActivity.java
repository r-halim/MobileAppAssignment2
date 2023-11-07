package com.example.assignment2;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.*;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    DatabaseHelper dbHelper;
    TextView output_result;
    EditText newAddressInput;
    Button addButton;
    Button deleteButton;
    Button updateButton;
    Geocoder geocoder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        dbHelper = new DatabaseHelper(this);
        dbHelper.insertDataFromTextFile(this);
        output_result = findViewById(R.id.output_result);

        geocoder = new Geocoder(this, Locale.getDefault());

        // Initialize UI elements after setting the content view
        newAddressInput = findViewById(R.id.new_address_input);
        addButton = findViewById(R.id.add_address);
        deleteButton = findViewById(R.id.delete_address);
        updateButton = findViewById(R.id.update_address);

        // Disable the "New Address" input field
        newAddressInput.setEnabled(false);
        // Disable the "Delete" button for the user
        deleteButton.setEnabled(false);
        // Disable the "New Address" input field
        newAddressInput.setEnabled(false);
        // Disable the "Update" button for the user
        updateButton.setEnabled(false);
        // Enable the "Add" button for the user to add the address
        addButton.setEnabled(false);
    }

    // Method invoked when "Check Address" button is clicked
    @SuppressLint("SetTextI18n")
    public void location_check(View view) {
        // Gets the address input from the user
        String address = ((EditText) findViewById(R.id.address_input)).getText().toString();

        // Checks if the address input is empty
        if (address.isEmpty()) {
            output_result.setText("Please enter an address!");
            return; // Exit the method early, since the input is not valid
        }

        // Add a log statement to display the queried address
        Log.d("MainActivityClass", "Queried address: " + address);

        // Perform a database query to check if the address exists in the database
        boolean isAddressFound = dbHelper.checkAddressInDatabase(address);

        // Add a log statement to display the result of the query
        Log.d("MainActivityClass", "Is address found: " + isAddressFound);

        if (isAddressFound) {
            // Address is found in the database; display the result with latitude and longitude
            displayAddressDetails(address);
            // Enable the "Delete" button for the user to delete the address
            deleteButton.setEnabled(true);
            // Enable the "New Address" input field
            newAddressInput.setEnabled(true);
            // Enable the "Update" button for the user to update the address
            updateButton.setEnabled(true);
            // Disable the "Add" button for the user
            addButton.setEnabled(false);
        } else {
            // Address is not found in the database; show a message to the user
            output_result.setText("Address is not found in the database.");
            // Disable the "Delete" button for the user
            deleteButton.setEnabled(false);
            // Disable the "New Address" input field
            newAddressInput.setEnabled(false);
            // Disable the "Update" button for the user
            updateButton.setEnabled(false);
            // Enable the "Add" button for the user to add the address
            addButton.setEnabled(true);
        }
    }

    @SuppressLint("SetTextI18n")
    // Method to display the address details (latitude and longitude)
    private void displayAddressDetails(String address) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DatabaseHelper.DATABASE_TABLE,
                new String[]{Location.COLUMN_LONGITUDE, Location.COLUMN_LATITUDE},
                Location.COLUMN_ADDRESS + " = ?",
                new String[]{address},
                null, null, null
        );

        if (cursor.moveToFirst()) {
            int longitudeIndex = cursor.getColumnIndex(Location.COLUMN_LONGITUDE);
            int latitudeIndex = cursor.getColumnIndex(Location.COLUMN_LATITUDE);

            if (longitudeIndex >= 0 && latitudeIndex >= 0) {
                double longitude = cursor.getDouble(longitudeIndex);
                double latitude = cursor.getDouble(latitudeIndex);
                // Format latitude and longitude to show only 4 decimal places
                @SuppressLint("DefaultLocale") String formattedLatitude = String.format("%.4f", latitude);
                @SuppressLint("DefaultLocale") String formattedLongitude = String.format("%.4f", longitude);
                String resultText = "Address is found. Latitude: " + formattedLatitude + ", Longitude: " + formattedLongitude;
                TextView output_result = findViewById(R.id.output_result);
                output_result.setText(resultText);
            } else {
                output_result.setText("Address details not available.");
            }
        } else {
            output_result.setText("Address is not found in the database.");
        }

        cursor.close();
        db.close();
    }

    // Method invoked when "Delete Address" button is clicked
    @SuppressLint("SetTextI18n")
    public void deleteAddress(View view) {
        String address = ((EditText) findViewById(R.id.address_input)).getText().toString();

        if (address.isEmpty()) {
            // Address input is empty, show a message to the user
            Toast.makeText(this, "Please enter an address to delete.", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isDeleted = dbHelper.deleteAddressFromDatabase(address);

        if (isDeleted) {
            // Address has been deleted successfully, show a success message
            Toast.makeText(this, "Address deleted successfully.", Toast.LENGTH_SHORT).show();
            output_result.setText("Address deleted successfully.");
        } else {
            // Address was not found in the database, show a message to the user
            Toast.makeText(this, "Address not found in the database.", Toast.LENGTH_SHORT).show();
            output_result.setText("Address not found in the database.");
        }
    }

    // The geocoding method used for both the update address and add address methods
    private Pair<Double, Double> geocodeAddress(String address) {
        double latitude = 0.0;
        double longitude = 0.0;

        try {
            List<Address> addresses = geocoder.getFromLocationName(address, 1);
            if (addresses != null && !addresses.isEmpty()) {
                latitude = addresses.get(0).getLatitude();
                longitude = addresses.get(0).getLongitude();
            }
        } catch (IOException e) {
            e.printStackTrace();
            // Handle the geocoding failure
        }

        return new Pair<>(latitude, longitude);
    }

    // Method invoked when "Add Address" button is clicked
    @SuppressLint("SetTextI18n")
    public void addAddressToDatabase(View view) {
        String address = ((EditText) findViewById(R.id.address_input)).getText().toString();

        if (address.isEmpty()) {
            // Address input is empty, show a message to the user
            output_result.setText("Please enter an address to add.");
            return;
        }

        Pair<Double, Double> latLng = geocodeAddress(address);

        double latitude = latLng.first;
        double longitude = latLng.second;

        // Adding the address to the database
        long result = dbHelper.addAddressToDatabase(address, latitude, longitude);

        if (result != -1) {
            // Address has been added successfully, show a success message
            Toast.makeText(this, "Address added successfully.", Toast.LENGTH_SHORT).show();
            output_result.setText("Address added successfully.");
        } else {
            // Address addition failed; show an error message
            Toast.makeText(this, "Failed to add the address to the database.", Toast.LENGTH_SHORT).show();
            output_result.setText("Failed to add the address to the database.");
        }

        // Disable the "Add" button after the operation
        Button addButton = findViewById(R.id.add_address);
        addButton.setEnabled(false);
    }

    // Method invoked when "Update Address" button is clicked
    @SuppressLint("SetTextI18n")
    public void updateAddress(View view) {
        String oldAddress = ((EditText) findViewById(R.id.address_input)).getText().toString();

        if (oldAddress.isEmpty()) {
            Toast.makeText(this, "Please enter the existing address to update.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Find the "New Address" input field
        EditText newAddressInput = findViewById(R.id.new_address_input);
        String newAddress = newAddressInput.getText().toString();

        if (newAddress.isEmpty()) {
            Toast.makeText(this, "Please enter a new address to update.", Toast.LENGTH_SHORT).show();
            return;
        }

        Pair<Double, Double> newLatLng = geocodeAddress(newAddress);

        double newLatitude = newLatLng.first;
        double newLongitude = newLatLng.second;

        boolean isUpdated = dbHelper.updateAddressInDatabase(oldAddress, newAddress, newLatitude, newLongitude);

        if (isUpdated) {
            // Address has been updated successfully, show a success message
            Toast.makeText(this, "Address updated successfully.", Toast.LENGTH_SHORT).show();
            output_result.setText("Address updated successfully.");
        } else {
            // Address was not found in the database, show a message to the user
            Toast.makeText(this, "Address not found or failed to update.", Toast.LENGTH_SHORT).show();
            output_result.setText("Address not found or failed to update.");
        }

        // Clear the "New Address" input field
        newAddressInput.setText("");
    }

}