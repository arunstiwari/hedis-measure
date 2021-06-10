package com.spectramedix;

import io.restassured.response.Response;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;

public class HedisMeasureVerifier {

    private static final String TAB_DELIMITER = "\t";

    public static void main(String[] args) {
        String path = System.getProperty("csv.report.path");
        String es_url = System.getProperty("es_url");
        String tablename = System.getProperty("tablename");
        System.out.println("path = " + path);
        System.out.println("es_url = " + es_url);
        System.out.println("tablename = " + tablename);
        
        if (path == null || es_url == null || tablename == null) {
            System.out.println("Specify the parameter -Dcsv.report.path" );
            System.out.println("Specify the parameter -Des_url" );
            System.out.println("Specify the parameter -Dtablename" );
            System.exit(-1);
        }

        List<List<String>> records = new ArrayList<>();
        try (Scanner scanner = new Scanner(new File(path));) {
            while (scanner.hasNextLine()) {
                records.add(getRecordFromLine(scanner.nextLine()));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        List<List<String>> recordsList = records.subList(2, records.size() - 1);
        System.out.println("recordsList.size() = " + recordsList.size());
        recordsList.stream()
//                .filter(rec -> rec.size() > 1)
                .forEach(rec -> {
                    System.out.println("rec = " + rec.get(0)+"WBC");
                    System.out.println("rec = " + rec.get(0).trim()+"WBC");
                    System.out.println("rec.get(0).trim().equalsIgnoreCase(tablename) = " + rec.get(0).trim().equalsIgnoreCase(tablename));
                });
        List<String> foundRecord = recordsList.stream()
                .filter(rec -> rec.size() >1)
                .filter(rec -> rec.get(0).trim().equalsIgnoreCase(tablename))
                .findFirst().orElse(null);
        if (foundRecord == null){
            System.out.println("No matching record with tablename found" );
            System.exit(1);
        }else{
            System.out.println("Matched record table name is : "+foundRecord.get(0));
            System.out.println("Matched record count is : "+foundRecord.get(1));
        }

        Response response = given()
                .accept(JSON)
                .queryParam("page", "1")
                .queryParam("limit", "50")
                .when()
                .get(es_url)
                .then()
                .statusCode(200)
                .contentType(JSON)
                .extract()
                .response();
        System.out.println("response = " + response.getBody().asString());
        String totalCount = response.body().jsonPath().get("hits.total");
        System.out.println("totalCount = " + totalCount);
        if (totalCount == foundRecord.get(1)){
            System.out.println("Count Records match correctly");
            System.exit(0);
        }else{
            System.out.println("Count Records does not match");
            System.exit(1);
        }
    }

    private static List<String> getRecordFromLine(String nextLine) {
        List<String> values = new ArrayList<String>();
        try (Scanner rowScanner = new Scanner(nextLine)) {
            rowScanner.useDelimiter(TAB_DELIMITER);
            while (rowScanner.hasNext()) {
                values.add(rowScanner.next());
            }
        }
        return values;
    }
}
