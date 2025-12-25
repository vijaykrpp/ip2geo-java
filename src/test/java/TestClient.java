package com.ip2geo;

public class TestClient {
    public static void main(String[] args) {
        Ip2GeoClient client = new Ip2GeoClient("API_KEY");
        Object response = client.lookup("8.8.8.8", null, null);
        System.out.println(response);
    }
}
