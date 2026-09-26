package com.thebackendguy.myandroidtestapp.data.remote

/*
 * Backend address. Keep one line commented out and swap them by hand.
 *
 * Local: a phone on USB reaches your PC's localhost after running
 * `adb reverse tcp:4000 tcp:4000` (once per connection). On Wi-Fi, use your
 * PC's IP instead, and add it to res/xml/network_security_config.xml.
 */
// const val BASE_URL = "http://localhost:4000"
const val BASE_URL = "https://api.thebackendguy.click"

const val API_URL = "$BASE_URL/api/v1/"
