package com.example.assignment2;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {
    // Database version
    private static final int DATABASE_VERSION = 4;

    // Database name
    private static final String DATABASE_NAME = "LocationDatabase";
    // Database Table
    public static final String DATABASE_TABLE = "LocationTable";

    // SQL statement to create the location table
    private static final String CREATE_TABLE = "CREATE TABLE " + Location.TABLE_NAME + " (" +
            Location.COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            Location.COLUMN_NAME + " TEXT, " +
            Location.COLUMN_LONGITUDE + " REAL, " +
            Location.COLUMN_LATITUDE + " REAL, " +
            Location.COLUMN_ADDRESS + " TEXT);";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Method to read data from a text file and insert it into the location table
    public void insertDataFromTextFile(Context context) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            AssetManager assetManager = context.getAssets();
            InputStream inputStream = assetManager.open("Locations.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                // Parse location name, longitude, and latitude from the text file
                String[] parts = line.split(",");
                if (parts.length >= 3) {
                    String locationName = parts[0].trim();
                    double longitude = Double.parseDouble(parts[1].trim());
                    double latitude = Double.parseDouble(parts[2].trim());

                    // Insert the data into the database
                    ContentValues values = new ContentValues();
                    values.put(Location.COLUMN_NAME, locationName);
                    values.put(Location.COLUMN_LONGITUDE, longitude);
                    values.put(Location.COLUMN_LATITUDE, latitude);

                    // Geocode the latitude and longitude to get the address
                    String address = geocodeLocation(context, latitude, longitude);

                    values.put(Location.COLUMN_ADDRESS, address);

                    // Log the query details
                    Log.d("DatabaseHelper: InsertDataFromTextFile", "LocationName: " + locationName +
                            " Longitude: " + longitude + " Latitude: " + latitude + " Address: " + address);


                    db.insert(DATABASE_TABLE, null, values);
                }
            }

            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            db.close();
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE);
            onCreate(db);
        }
    }

    // Method to geocode latitude and longitude into an address
    private String geocodeLocation(Context context, double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        String address = "Address not found"; // Default value if geocoding fails

        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address returnedAddress = addresses.get(0);

                // Extract building number, street, and city from the returned address
                String buildingNumber = returnedAddress.getSubThoroughfare();
                String street = returnedAddress.getThoroughfare();
                String city = returnedAddress.getLocality();

                StringBuilder addressBuilder = new StringBuilder();

                if (buildingNumber != null) {
                    addressBuilder.append(buildingNumber);
                    addressBuilder.append(" ");
                }

                if (street != null) {
                    addressBuilder.append(street);
                    addressBuilder.append(", ");
                }

                if (city != null) {
                    addressBuilder.append(city);
                }

                address = addressBuilder.toString();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("Geocoding Error", "Error during geocoding: " + e.getMessage());
        }

        Log.d("Geocoding Result", "Latitude: " + latitude + ", Longitude: " + longitude + ", Address: " + address);

        return address;
    }

    // Method to check if address is in the database
    public boolean checkAddressInDatabase(String address) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(
                DATABASE_TABLE, // Use the table name from the DatabaseHelper
                new String[]{Location.COLUMN_NAME, Location.COLUMN_LONGITUDE, Location.COLUMN_LATITUDE},
                Location.COLUMN_ADDRESS + " = ?",
                new String[]{address},
                null, null, null
        );

        boolean isFound = cursor.moveToFirst();
        cursor.close();
        db.close();
        return isFound;
    }

    // Method to delete address from database
    public boolean deleteAddressFromDatabase(String address) {
        SQLiteDatabase db = getWritableDatabase();
        String whereClause = Location.COLUMN_ADDRESS + " = ?";
        String[] whereArgs = {address};

        try {
            int rowsDeleted = db.delete(DATABASE_TABLE, whereClause, whereArgs);
            if (rowsDeleted > 0) {
                Log.d("DatabaseHelper", "Address deleted: " + address);
            } else {
                Log.d("DatabaseHelper", "Address not found: " + address);
            }
            return rowsDeleted > 0;
        } finally {
            db.close();
        }
    }

    // Method to add address to database
    public long addAddressToDatabase(String address, double latitude, double longitude) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(Location.COLUMN_NAME, address);
        values.put(Location.COLUMN_LONGITUDE, longitude);
        values.put(Location.COLUMN_LATITUDE, latitude);
        values.put(Location.COLUMN_ADDRESS, address);

        long result = db.insert(DATABASE_TABLE, null, values);
        if (result != -1) {
            Log.d("DatabaseHelper", "Address added: " + address + " ;Longitude:" + longitude + " ;Latitude" + latitude);
        } else {
            Log.d("DatabaseHelper", "Failed to add address: " + address);
        }

        return result;
    }

    // Method to update address in database
    public boolean updateAddressInDatabase(String oldAddress, String newAddress, double newLatitude, double newLongitude) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(Location.COLUMN_NAME, newAddress); // Use newAddress here
        values.put(Location.COLUMN_LONGITUDE, newLongitude);
        values.put(Location.COLUMN_LATITUDE, newLatitude);

        String whereClause = Location.COLUMN_ADDRESS + " = ?";
        String[] whereArgs = {oldAddress}; // Use oldAddress here

        try {
            int rowsUpdated = db.update(DATABASE_TABLE, values, whereClause, whereArgs);

            // Log statements to see what's happening
            Log.d("DatabaseHelper", "Updating address: " + oldAddress + " to " + newAddress);
            Log.d("DatabaseHelper", "Rows updated: " + rowsUpdated);

            return rowsUpdated > 0;
        } finally {
            db.close();
        }
    }


}